import Foundation

/// Shares challenges through a Supabase project (PostgREST over HTTPS).
/// Enabled when `SupabaseURL` and `SupabaseAnonKey` are set in Info.plist —
/// see docs/BACKEND.md for the table and access rules.
final class SupabaseChallengeService: ChallengeService {
    private let baseURL: URL
    private let apiKey: String
    private let session: URLSession

    init(baseURL: URL, apiKey: String, session: URLSession = .shared) {
        self.baseURL = baseURL
        self.apiKey = apiKey
        self.session = session
    }

    /// Reads the config from Info.plist; nil when not set up.
    static func fromBundle(_ bundle: Bundle = .main) -> SupabaseChallengeService? {
        guard let urlString = bundle.object(forInfoDictionaryKey: "SupabaseURL") as? String,
              let url = URL(string: urlString), !urlString.isEmpty,
              let key = bundle.object(forInfoDictionaryKey: "SupabaseAnonKey") as? String, !key.isEmpty
        else { return nil }
        return SupabaseChallengeService(baseURL: url, apiKey: key)
    }

    var isOnline: Bool { true }

    /// One row per challenge; participants stored as JSON.
    private struct Row: Codable {
        let id: String
        let code: String
        let title: String
        let target: Int
        let start: Date
        let end: Date
        var participants: [Challenge.Participant]
        let participant_ids: [String]

        init(_ c: Challenge) {
            id = c.id; code = c.code; title = c.title; target = c.target
            start = c.start; end = c.end; participants = c.participants
            participant_ids = c.participants.map(\.id)
        }

        var challenge: Challenge {
            Challenge(id: id, code: code, title: title, target: target, start: start, end: end, participants: participants)
        }
    }

    private let encoder: JSONEncoder = { let e = JSONEncoder(); e.dateEncodingStrategy = .iso8601; return e }()
    private let decoder: JSONDecoder = {
        let d = JSONDecoder()
        d.dateDecodingStrategy = .custom { dec in
            let s = try dec.singleValueContainer().decode(String.self)
            let f = ISO8601DateFormatter()
            f.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
            if let date = f.date(from: s) { return date }
            f.formatOptions = [.withInternetDateTime]
            if let date = f.date(from: s) { return date }
            throw DecodingError.dataCorruptedError(in: try dec.singleValueContainer(), debugDescription: "Bad date \(s)")
        }
        return d
    }()

    private func request(_ path: String, method: String = "GET", query: [URLQueryItem] = [],
                         body: Data? = nil, prefer: String? = nil) async throws -> Data {
        var comps = URLComponents(url: baseURL.appendingPathComponent("rest/v1/\(path)"), resolvingAgainstBaseURL: false)!
        if !query.isEmpty { comps.queryItems = query }
        var req = URLRequest(url: comps.url!)
        req.httpMethod = method
        req.setValue(apiKey, forHTTPHeaderField: "apikey")
        req.setValue("Bearer \(apiKey)", forHTTPHeaderField: "Authorization")
        req.setValue("application/json", forHTTPHeaderField: "Content-Type")
        if let prefer { req.setValue(prefer, forHTTPHeaderField: "Prefer") }
        req.httpBody = body
        do {
            let (data, response) = try await session.data(for: req)
            guard let http = response as? HTTPURLResponse, (200..<300).contains(http.statusCode) else {
                throw ChallengeError.network(String(data: data, encoding: .utf8) ?? "HTTP error")
            }
            return data
        } catch let e as ChallengeError {
            throw e
        } catch {
            throw ChallengeError.network(error.localizedDescription)
        }
    }

    func list(for userId: String) async throws -> [Challenge] {
        let data = try await request("challenges", query: [
            URLQueryItem(name: "participant_ids", value: "cs.{\(userId)}"),
            URLQueryItem(name: "order", value: "end.desc"),
        ])
        return try decoder.decode([Row].self, from: data).map(\.challenge)
    }

    func create(_ c: Challenge) async throws {
        _ = try await request("challenges", method: "POST", body: try encoder.encode(Row(c)), prefer: "return=minimal")
    }

    private func fetch(code: String) async throws -> Challenge {
        let data = try await request("challenges", query: [URLQueryItem(name: "code", value: "eq.\(code)")])
        guard let row = try decoder.decode([Row].self, from: data).first else { throw ChallengeError.notFound }
        return row.challenge
    }

    private func put(_ c: Challenge) async throws {
        _ = try await request("challenges", method: "PATCH",
                              query: [URLQueryItem(name: "id", value: "eq.\(c.id)")],
                              body: try encoder.encode(Row(c)), prefer: "return=minimal")
    }

    func join(code: String, as participant: Challenge.Participant) async throws -> Challenge {
        var c = try await fetch(code: code)
        if !c.participants.contains(where: { $0.id == participant.id }) {
            c.participants.append(participant)
            try await put(c)
        }
        return c
    }

    /// Read-modify-write of this user's own entry. Two friends reporting in the same
    /// instant could overwrite each other; the next report corrects it.
    func report(challengeId: String, participant: Challenge.Participant) async throws {
        let data = try await request("challenges", query: [URLQueryItem(name: "id", value: "eq.\(challengeId)")])
        guard var c = try decoder.decode([Row].self, from: data).first?.challenge,
              let i = c.participants.firstIndex(where: { $0.id == participant.id }) else { return }
        guard c.participants[i] != participant else { return }
        c.participants[i] = participant
        try await put(c)
    }
}
