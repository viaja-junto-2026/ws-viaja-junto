// Resume a cobertura do JaCoCo em Markdown para o "job summary" do GitHub Actions.
// Uso: node .github/scripts/coverage-summary.js >> "$GITHUB_STEP_SUMMARY"
const fs = require("fs");

const csvPath = "target/site/jacoco/jacoco.csv";
if (!fs.existsSync(csvPath)) {
  console.log("## Cobertura de testes\n\nRelatório do JaCoCo não encontrado (`" + csvPath + "`).");
  process.exit(0);
}

// Colunas do CSV: GROUP,PACKAGE,CLASS,INSTRUCTION_MISSED,INSTRUCTION_COVERED,BRANCH_MISSED,BRANCH_COVERED,LINE_MISSED,LINE_COVERED,...
const rows = fs.readFileSync(csvPath, "utf8").trim().split("\n").slice(1).map(line => line.split(","));

const total = { lineMissed: 0, lineCovered: 0, branchMissed: 0, branchCovered: 0 };
const byPackage = {};
for (const row of rows) {
  const pkg = row[1].replace("com.viajajunto.api", "").replace(/^\./, "") || "(raiz)";
  const branchMissed = Number(row[5]);
  const branchCovered = Number(row[6]);
  const lineMissed = Number(row[7]);
  const lineCovered = Number(row[8]);
  total.lineMissed += lineMissed;
  total.lineCovered += lineCovered;
  total.branchMissed += branchMissed;
  total.branchCovered += branchCovered;
  byPackage[pkg] = byPackage[pkg] || { lineMissed: 0, lineCovered: 0 };
  byPackage[pkg].lineMissed += lineMissed;
  byPackage[pkg].lineCovered += lineCovered;
}

const pct = (covered, missed) => (covered + missed === 0 ? "-" : (100 * covered / (covered + missed)).toFixed(1) + "%");

console.log("## Cobertura de testes (JaCoCo)\n");
console.log("| Métrica | Cobertura |");
console.log("| :--- | ---: |");
console.log("| Linhas | " + pct(total.lineCovered, total.lineMissed) + " (" + total.lineCovered + "/" + (total.lineCovered + total.lineMissed) + ") |");
console.log("| Branches | " + pct(total.branchCovered, total.branchMissed) + " (" + total.branchCovered + "/" + (total.branchCovered + total.branchMissed) + ") |");
console.log("\n<details><summary>Por pacote (linhas)</summary>\n");
console.log("| Pacote | Cobertura |");
console.log("| :--- | ---: |");
Object.entries(byPackage)
  .sort((a, b) => a[0].localeCompare(b[0]))
  .forEach(([pkg, v]) => console.log("| `" + pkg + "` | " + pct(v.lineCovered, v.lineMissed) + " |"));
console.log("\n</details>");
console.log("\nO relatório HTML completo está no artefato `jacoco-report`.");
