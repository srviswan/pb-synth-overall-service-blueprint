const views = ["tradeList", "contractEntry", "positionBrowser"];
const tabButtons = document.querySelectorAll(".tab-btn");

tabButtons.forEach((btn) => {
  btn.addEventListener("click", () => {
    const target = btn.dataset.view;
    tabButtons.forEach((b) => b.classList.remove("active"));
    btn.classList.add("active");
    views.forEach((id) => {
      document.getElementById(id).classList.toggle("active", id === target);
    });
  });
});

const trades = [
  { tradeId: "TRD-2026-001245", client: "Alpha Fund", security: "AAPL", notional: "10,000,000", currency: "USD", status: "Draft", effective: "2026-01-15" },
  { tradeId: "TRD-2026-001246", client: "Beta Asset Mgmt", security: "AAPL", notional: "5,500,000", currency: "EUR", status: "Validated", effective: "2026-02-01" },
  { tradeId: "TRD-2026-001247", client: "Gamma Capital", security: "AAPL", notional: "750,000,000", currency: "JPY", status: "Pending Review", effective: "2026-02-10" },
  { tradeId: "TRD-2026-001248", client: "London Fund", security: "AAPL", notional: "2,500,000", currency: "GBP", status: "Published", effective: "2026-02-20" },
  { tradeId: "TRD-2026-001249", client: "Alpha Fund", security: "MSFT", notional: "12,000,000", currency: "USD", status: "Draft", effective: "2026-03-05" }
];

const tradeBody = document.getElementById("tradeTableBody");
const tradeIdFilter = document.getElementById("tradeIdFilter");
const tradeStatusFilter = document.getElementById("tradeStatusFilter");
const tradeCurrencyFilter = document.getElementById("tradeCurrencyFilter");

function renderTrades() {
  const idQ = tradeIdFilter.value.trim().toLowerCase();
  const statusQ = tradeStatusFilter.value;
  const ccyQ = tradeCurrencyFilter.value;

  const filtered = trades.filter((t) => {
    const idMatch = !idQ || t.tradeId.toLowerCase().includes(idQ);
    const statusMatch = !statusQ || t.status === statusQ;
    const ccyMatch = !ccyQ || t.currency === ccyQ;
    return idMatch && statusMatch && ccyMatch;
  });

  tradeBody.innerHTML = filtered
    .map(
      (t) => `
      <tr>
        <td>${t.tradeId}</td>
        <td>${t.client}</td>
        <td>${t.security}</td>
        <td>${t.notional}</td>
        <td>${t.currency}</td>
        <td>${t.status}</td>
        <td>${t.effective}</td>
      </tr>
    `
    )
    .join("");
}

[tradeIdFilter, tradeStatusFilter, tradeCurrencyFilter].forEach((el) =>
  el.addEventListener("input", renderTrades)
);
[tradeStatusFilter, tradeCurrencyFilter].forEach((el) =>
  el.addEventListener("change", renderTrades)
);

renderTrades();
