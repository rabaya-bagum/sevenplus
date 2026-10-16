import SwiftUI
import StoreKit

struct PaywallView: View {
    @EnvironmentObject var premium: PremiumStore
    @Environment(\.dismiss) private var dismiss
    @State private var buying = false

    private let rows: [(String, Bool, Bool)] = [
        ("Classic 7-minute circuit", true, true),
        ("Daily workout, 5 and 7 minutes", true, true),
        ("Quiet mode and limitations", true, true),
        ("Custom workouts", true, true),
        ("10, 15 and 20-minute workouts", false, true),
        ("Unlimited custom workouts", false, true),
        ("Adaptive plans (coming soon)", false, true),
    ]

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 24) {
                    Image(systemName: "bolt.heart.fill").font(.system(size: 56)).foregroundStyle(.orange)
                        .accessibilityHidden(true)
                    Text("SevenPlus Premium").font(.largeTitle.bold())
                    Text("Longer workouts and no limits. The basics stay free.")
                        .multilineTextAlignment(.center).foregroundStyle(.secondary)

                    Grid(alignment: .leading, horizontalSpacing: 16, verticalSpacing: 10) {
                        GridRow {
                            Text("")
                            Text("Free").font(.caption.bold())
                            Text("Premium").font(.caption.bold())
                        }
                        ForEach(rows, id: \.0) { row in
                            GridRow {
                                Text(row.0)
                                mark(row.1)
                                mark(row.2)
                            }
                        }
                    }
                    .padding()
                    .background(.secondary.opacity(0.08), in: RoundedRectangle(cornerRadius: 16))

                    if premium.isPremium {
                        Label("You're on Premium. Thanks!", systemImage: "checkmark.seal.fill").foregroundStyle(.green)
                    } else if premium.products.isEmpty {
                        ProgressView("Loading prices…")
                    } else {
                        ForEach(premium.products, id: \.id) { product in
                            Button {
                                buying = true
                                Task {
                                    if await premium.buy(product) { dismiss() }
                                    buying = false
                                }
                            } label: {
                                VStack {
                                    Text(product.displayName).font(.headline)
                                    Text(priceLine(product)).font(.subheadline)
                                }
                                .frame(maxWidth: .infinity).padding(.vertical, 6)
                            }
                            .buttonStyle(.borderedProminent)
                            .disabled(buying)
                        }
                    }

                    if let error = premium.lastError {
                        Text(error).font(.footnote).foregroundStyle(.red)
                    }

                    Button("Restore purchases") { Task { await premium.restore() } }
                        .font(.footnote)
                    Text("Subscriptions renew automatically until cancelled in Settings. Cancel at least 24 hours before renewal to avoid being charged.")
                        .font(.caption2).foregroundStyle(.secondary).multilineTextAlignment(.center)
                }
                .padding(24)
            }
            .toolbar { Button("Not now") { dismiss() } }
        }
    }

    private func mark(_ on: Bool) -> some View {
        Image(systemName: on ? "checkmark" : "minus")
            .foregroundStyle(on ? Color.green : Color.secondary)
            .accessibilityLabel(on ? "Included" : "Not included")
    }

    private func priceLine(_ p: Product) -> String {
        var line = p.displayPrice
        if let period = p.subscription?.subscriptionPeriod {
            line += period.unit == .year ? " / year" : " / month"
        }
        if let intro = p.subscription?.introductoryOffer, intro.paymentMode == .freeTrial {
            line = "\(intro.period.value)-\(intro.period.unit == .week ? "week" : "day") free trial, then " + line
        }
        return line
    }
}
