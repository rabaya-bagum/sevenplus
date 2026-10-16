import SwiftUI
import Charts

struct ProgressTabView: View {
    @EnvironmentObject var store: SessionStore

    private var week: [Stats.Day] { Stats.dailyMinutes(store.sessions) }

    var body: some View {
        NavigationStack {
            List {
                Section {
                    HStack {
                        stat("\(store.streak)", "Current streak")
                        stat("\(Stats.bestStreak(store.sessions))", "Best streak")
                        stat("\(store.sessions.filter(\.completed).count)", "Workouts")
                        stat("\(Stats.totalMinutes(store.sessions))", "Minutes")
                    }
                }

                Section("Last 7 days") {
                    Chart(week) { day in
                        BarMark(x: .value("Day", day.date, unit: .day),
                                y: .value("Minutes", day.minutes))
                            .foregroundStyle(.tint)
                    }
                    .chartXAxis {
                        AxisMarks(values: .stride(by: .day)) { _ in
                            AxisValueLabel(format: .dateTime.weekday(.narrow))
                        }
                    }
                    .chartYAxisLabel("min")
                    .frame(height: 160)
                    .padding(.vertical, 8)
                    .accessibilityLabel("Minutes trained this week: \(week.map(\.minutes).reduce(0, +))")
                }

                Section("History") {
                    if store.sessions.isEmpty {
                        Text("Finish a workout and it will show up here.").foregroundStyle(.secondary)
                    }
                    ForEach(store.sessions.prefix(30)) { s in
                        HStack {
                            VStack(alignment: .leading) {
                                Text(s.date, format: .dateTime.weekday(.wide).month().day())
                                if let d = s.difficulty {
                                    Text(d.rawValue).font(.caption).foregroundStyle(.secondary)
                                }
                            }
                            Spacer()
                            Text(s.completed ? "\(s.durationSeconds / 60) min" : "Stopped early")
                                .foregroundStyle(.secondary)
                        }
                    }
                }
            }
            .navigationTitle("Progress")
        }
    }

    private func stat(_ value: String, _ label: String) -> some View {
        VStack(spacing: 2) {
            Text(value).font(.title2.bold()).monospacedDigit()
            Text(label).font(.caption2).foregroundStyle(.secondary).multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity)
        .accessibilityElement(children: .combine)
    }
}
