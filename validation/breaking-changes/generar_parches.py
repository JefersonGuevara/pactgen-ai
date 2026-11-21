"""Genera los 35 parches de validation/breaking-changes/<servicio>/<ID>.patch (prerregistro del 20 nov).

Cada mutante es una lista de sustituciones (archivo, ancla, reemplazo) sobre el código del proveedor: el clon de
petclinic en v3.4.1 (P1, P2; ../providers/petclinic) o el portado de piggymetrics (P3-P5). Se aplica sobre el árbol
de trabajo, se guarda `git diff` (relativo a la raíz del clon o del portado) y se restaura el archivo con
`git checkout`. Los árboles deben estar limpios en src/main.

Uso (desde la raíz de pactgen-ai): python -I validation/breaking-changes/generar_parches.py
"""
import subprocess, sys, pathlib

REPO = pathlib.Path(__file__).resolve().parents[2]
PETCLINIC = REPO.parent / "providers" / "petclinic"
PIGGY = REPO / "validation/provider-verification/piggymetrics"
OUT = REPO / "validation/breaking-changes"

CUST = "spring-petclinic-customers-service/src/main/java/org/springframework/samples/petclinic/customers"
VIS = "spring-petclinic-visits-service/src/main/java/org/springframework/samples/petclinic/visits"
ACC = "src/main/java/com/piggymetrics/account"
STA = "src/main/java/com/piggymetrics/statistics"
AUT = "src/main/java/com/piggymetrics/auth"

def imp(after, new):
    """Inserta una línea de import tras otra existente."""
    return (after, after + "\n" + new)

MUTANTS = {
    # ------------------------------------------------------------------ P1 customers-service
    ("customers-service", PETCLINIC): {
        "BC1": [(f"{CUST}/model/Owner.java", *imp("import jakarta.persistence.*;", "import com.fasterxml.jackson.annotation.JsonIgnore;")),
                (f"{CUST}/model/Owner.java", '    @Column(name = "last_name")\n', '    @JsonIgnore\n    @Column(name = "last_name")\n')],
        "BC2": [(f"{CUST}/model/Owner.java", *imp("import jakarta.persistence.*;",
                 "import com.fasterxml.jackson.databind.annotation.JsonSerialize;\nimport com.fasterxml.jackson.databind.ser.std.ToStringSerializer;")),
                (f"{CUST}/model/Owner.java", "    @Id\n", "    @Id\n    @JsonSerialize(using = ToStringSerializer.class)\n")],
        "BC3": [(f"{CUST}/web/OwnerResource.java", '    @GetMapping(value = "/{ownerId}")\n',
                 '    @GetMapping(value = "/{ownerId}")\n    @ResponseStatus(HttpStatus.NO_CONTENT)\n')],
        "BC4": [(f"{CUST}/web/OwnerRequest.java", "                           String telephone\n",
                 "                           String telephone,\n                           @NotBlank String email\n")],
        "BC5": [(f"{CUST}/web/OwnerResource.java", '    @GetMapping(value = "/{ownerId}")\n', '    @GetMapping(value = "/{ownerId}/profile")\n')],
        "NB1": [(f"{CUST}/model/Owner.java", "    public String getTelephone() {\n",
                 "    public String getFullName() {\n        return this.firstName + \" \" + this.lastName;\n    }\n\n    public String getTelephone() {\n")],
        "NB2": [(f"{CUST}/web/OwnerResource.java", "    /**\n     * Read List of Owners\n",
                 "    /**\n     * Count Owners\n     */\n    @GetMapping(value = \"/count\")\n    public long countOwners() {\n"
                 "        return ownerRepository.count();\n    }\n\n    /**\n     * Read List of Owners\n")],
    },
    # ------------------------------------------------------------------ P2 visits-service
    ("visits-service", PETCLINIC): {
        "BC1": [(f"{VIS}/model/Visit.java", *imp("import com.fasterxml.jackson.annotation.JsonFormat;", "import com.fasterxml.jackson.annotation.JsonIgnore;")),
                (f"{VIS}/model/Visit.java", '    @Column(name = "pet_id")\n', '    @JsonIgnore\n    @Column(name = "pet_id")\n')],
        "BC2": [(f"{VIS}/model/Visit.java", *imp("import com.fasterxml.jackson.annotation.JsonFormat;",
                 "import com.fasterxml.jackson.databind.annotation.JsonSerialize;\nimport com.fasterxml.jackson.databind.ser.std.ToStringSerializer;")),
                (f"{VIS}/model/Visit.java", "    @Id\n", "    @Id\n    @JsonSerialize(using = ToStringSerializer.class)\n")],
        "BC3": [(f"{VIS}/web/VisitResource.java", '    @GetMapping("pets/visits")\n', '    @GetMapping("pets/visits")\n    @ResponseStatus(HttpStatus.NO_CONTENT)\n')],
        "BC4": [(f"{VIS}/model/Visit.java", *imp("import com.fasterxml.jackson.annotation.JsonFormat;",
                 "import com.fasterxml.jackson.annotation.JsonProperty;\nimport jakarta.validation.constraints.NotNull;")),
                (f"{VIS}/model/Visit.java", '    @Column(name = "pet_id")\n    private int petId;\n',
                 '    @Column(name = "pet_id")\n    private int petId;\n\n    @Transient\n    @NotNull\n'
                 '    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)\n    private String vet;\n')],
        "BC5": [(f"{VIS}/web/VisitResource.java", '    @GetMapping("pets/visits")\n', '    @GetMapping("pets/visit-history")\n')],
        "NB1": [(f"{VIS}/model/Visit.java", "    public String getDescription() {\n",
                 "    public Integer getYear() {\n        if (this.date == null) return null;\n"
                 "        java.util.Calendar c = java.util.Calendar.getInstance();\n        c.setTime(this.date);\n"
                 "        return c.get(java.util.Calendar.YEAR);\n    }\n\n    public String getDescription() {\n")],
        "NB2": [(f"{VIS}/web/VisitResource.java", '    @GetMapping("pets/visits")\n',
                 '    @GetMapping("visits/count")\n    public long count() {\n        return visitRepository.count();\n    }\n\n'
                 '    @GetMapping("pets/visits")\n')],
    },
    # ------------------------------------------------------------------ P3 account-service
    ("account-service", PIGGY): {
        "BC1": [(f"{ACC}/domain/Account.java", *imp("import jakarta.validation.Valid;", "import com.fasterxml.jackson.annotation.JsonIgnore;")),
                (f"{ACC}/domain/Account.java", "\tprivate String name;\n", "\t@JsonIgnore\n\tprivate String name;\n")],
        "BC2": [(f"{ACC}/domain/Account.java", *imp("import jakarta.validation.Valid;", "import com.fasterxml.jackson.annotation.JsonFormat;")),
                (f"{ACC}/domain/Account.java", "\tprivate Date lastSeen;\n", "\t@JsonFormat(shape = JsonFormat.Shape.NUMBER)\n\tprivate Date lastSeen;\n")],
        "BC3": [(f"{ACC}/controller/AccountController.java", *imp("import org.springframework.beans.factory.annotation.Autowired;",
                 "import org.springframework.http.HttpStatus;")),
                (f"{ACC}/controller/AccountController.java", '\t@RequestMapping(path = "/{name}", method = RequestMethod.GET)\n',
                 '\t@RequestMapping(path = "/{name}", method = RequestMethod.GET)\n\t@ResponseStatus(HttpStatus.NO_CONTENT)\n')],
        "BC4": [(f"{ACC}/domain/User.java", "\tpublic String getUsername() {\n",
                 "\t@NotNull\n\tprivate String email;\n\n\tpublic String getEmail() {\n\t\treturn email;\n\t}\n\n"
                 "\tpublic void setEmail(String email) {\n\t\tthis.email = email;\n\t}\n\n\tpublic String getUsername() {\n")],
        "BC5": [(f"{ACC}/controller/AccountController.java", '\t@RequestMapping(path = "/{name}", method = RequestMethod.GET)\n',
                 '\t@RequestMapping(path = "/by-name/{name}", method = RequestMethod.GET)\n')],
        "NB1": [(f"{ACC}/domain/Account.java", "\tpublic String getNote() {\n",
                 "\tpublic int getItemCount() {\n\t\treturn (incomes == null ? 0 : incomes.size()) + (expenses == null ? 0 : expenses.size());\n"
                 "\t}\n\n\tpublic String getNote() {\n")],
        "NB2": [(f"{ACC}/controller/AccountController.java", *imp("import com.piggymetrics.account.domain.Account;",
                 "import com.piggymetrics.account.domain.Currency;")),
                (f"{ACC}/controller/AccountController.java", '\t@RequestMapping(path = "/current", method = RequestMethod.GET)\n',
                 '\t@RequestMapping(path = "/currencies", method = RequestMethod.GET)\n\tpublic Currency[] getCurrencies() {\n'
                 '\t\treturn Currency.values();\n\t}\n\n\t@RequestMapping(path = "/current", method = RequestMethod.GET)\n')],
    },
    # ------------------------------------------------------------------ P4 statistics-service
    ("statistics-service", PIGGY): {
        "BC1": [(f"{STA}/domain/timeseries/DataPoint.java", *imp("import com.piggymetrics.statistics.domain.Currency;",
                 "import com.fasterxml.jackson.annotation.JsonIgnore;")),
                (f"{STA}/domain/timeseries/DataPoint.java", "\tprivate Map<Currency, BigDecimal> rates;\n",
                 "\t@JsonIgnore\n\tprivate Map<Currency, BigDecimal> rates;\n")],
        "BC2": [(f"{STA}/domain/timeseries/ItemMetric.java", *imp("import com.piggymetrics.statistics.domain.TimePeriod;",
                 "import com.fasterxml.jackson.databind.annotation.JsonSerialize;\nimport com.fasterxml.jackson.databind.ser.std.ToStringSerializer;")),
                (f"{STA}/domain/timeseries/ItemMetric.java", "\tprivate BigDecimal amount;\n",
                 "\t@JsonSerialize(using = ToStringSerializer.class)\n\tprivate BigDecimal amount;\n")],
        "BC3": [(f"{STA}/controller/StatisticsController.java", *imp("import org.springframework.beans.factory.annotation.Autowired;",
                 "import org.springframework.http.HttpStatus;")),
                (f"{STA}/controller/StatisticsController.java", '\t@RequestMapping(value = "/{accountName}", method = RequestMethod.GET)\n',
                 '\t@RequestMapping(value = "/{accountName}", method = RequestMethod.GET)\n\t@ResponseStatus(HttpStatus.NO_CONTENT)\n')],
        "BC4": [(f"{STA}/domain/Account.java", "\tpublic List<Item> getIncomes() {\n",
                 "\t@NotNull\n\tprivate Boolean active;\n\n\tpublic Boolean getActive() {\n\t\treturn active;\n\t}\n\n"
                 "\tpublic void setActive(Boolean active) {\n\t\tthis.active = active;\n\t}\n\n\tpublic List<Item> getIncomes() {\n")],
        "BC5": [(f"{STA}/controller/StatisticsController.java", '\t@RequestMapping(value = "/{accountName}", method = RequestMethod.PUT)\n',
                 '\t@RequestMapping(value = "/accounts/{accountName}", method = RequestMethod.PUT)\n')],
        "NB1": [(f"{STA}/domain/timeseries/DataPoint.java", "\tpublic Map<StatisticMetric, BigDecimal> getStatistics() {\n",
                 "\tpublic int getItemCount() {\n\t\treturn (incomes == null ? 0 : incomes.size()) + (expenses == null ? 0 : expenses.size());\n"
                 "\t}\n\n\tpublic Map<StatisticMetric, BigDecimal> getStatistics() {\n")],
        "NB2": [(f"{STA}/controller/StatisticsController.java", '\t@RequestMapping(value = "/current", method = RequestMethod.GET)\n',
                 '\t@RequestMapping(value = "/count", method = RequestMethod.GET)\n\tpublic int countCurrentAccountStatistics(Principal principal) {\n'
                 '\t\treturn statisticsService.findByAccountName(principal.getName()).size();\n\t}\n\n'
                 '\t@RequestMapping(value = "/current", method = RequestMethod.GET)\n')],
    },
    # ------------------------------------------------------------------ P5 auth-service
    # BC1, BC2 y NB1 cambian la serialización de un objeto de Spring Security (el Principal): el controlador
    # devuelve el mismo JSON convertido a Map (mismo ObjectMapper) con el cambio aplicado.
    ("auth-service", PIGGY): {
        "BC1": [(f"{AUT}/controller/UserController.java", *imp("import com.piggymetrics.auth.service.UserService;",
                 "import com.fasterxml.jackson.databind.ObjectMapper;\nimport java.util.Map;")),
                (f"{AUT}/controller/UserController.java", "\tpublic Principal getUser(Principal principal) {\n\t\treturn principal;\n",
                 "\tpublic Map<String, Object> getUser(Principal principal) {\n\t\tMap<String, Object> body = mapper.convertValue(principal, Map.class);\n"
                 "\t\tbody.remove(\"name\");\n\t\treturn body;\n"),
                (f"{AUT}/controller/UserController.java", "\tprivate UserService userService;\n",
                 "\tprivate UserService userService;\n\n\t@Autowired\n\tprivate ObjectMapper mapper;\n")],
        "BC2": [(f"{AUT}/controller/UserController.java", *imp("import com.piggymetrics.auth.service.UserService;",
                 "import com.fasterxml.jackson.databind.ObjectMapper;\nimport java.util.Map;")),
                (f"{AUT}/controller/UserController.java", "\tpublic Principal getUser(Principal principal) {\n\t\treturn principal;\n",
                 "\tpublic Map<String, Object> getUser(Principal principal) {\n\t\tMap<String, Object> body = mapper.convertValue(principal, Map.class);\n"
                 "\t\tbody.put(\"authenticated\", String.valueOf(body.get(\"authenticated\")));\n\t\treturn body;\n"),
                (f"{AUT}/controller/UserController.java", "\tprivate UserService userService;\n",
                 "\tprivate UserService userService;\n\n\t@Autowired\n\tprivate ObjectMapper mapper;\n")],
        "BC3": [(f"{AUT}/controller/UserController.java", *imp("import org.springframework.beans.factory.annotation.Autowired;",
                 "import org.springframework.http.HttpStatus;")),
                (f"{AUT}/controller/UserController.java", *imp("import org.springframework.web.bind.annotation.RequestMethod;",
                 "import org.springframework.web.bind.annotation.ResponseStatus;")),
                (f"{AUT}/controller/UserController.java", '\t@RequestMapping(value = "/current", method = RequestMethod.GET)\n',
                 '\t@RequestMapping(value = "/current", method = RequestMethod.GET)\n\t@ResponseStatus(HttpStatus.NO_CONTENT)\n')],
        "BC4": [(f"{AUT}/domain/User.java", *imp("import org.springframework.security.core.userdetails.UserDetails;",
                 "\nimport jakarta.validation.constraints.NotNull;")),
                (f"{AUT}/domain/User.java", "\tprivate String password;\n",
                 "\tprivate String password;\n\n\t@NotNull\n\tprivate String email;\n\n\tpublic String getEmail() {\n\t\treturn email;\n\t}\n\n"
                 "\tpublic void setEmail(String email) {\n\t\tthis.email = email;\n\t}\n")],
        # La ruta de la clase (/users) se reparte entre los métodos para que solo cambie la del POST
        "BC5": [(f"{AUT}/controller/UserController.java", '@RequestMapping("/users")\n', ''),
                (f"{AUT}/controller/UserController.java", '\t@RequestMapping(value = "/current", method = RequestMethod.GET)\n',
                 '\t@RequestMapping(value = "/users/current", method = RequestMethod.GET)\n'),
                (f"{AUT}/controller/UserController.java", '\t@RequestMapping(method = RequestMethod.POST)\n',
                 '\t@RequestMapping(value = "/accounts", method = RequestMethod.POST)\n')],
        "NB1": [(f"{AUT}/controller/UserController.java", *imp("import com.piggymetrics.auth.service.UserService;",
                 "import com.fasterxml.jackson.databind.ObjectMapper;\nimport java.util.Map;")),
                (f"{AUT}/controller/UserController.java", "\tpublic Principal getUser(Principal principal) {\n\t\treturn principal;\n",
                 "\tpublic Map<String, Object> getUser(Principal principal) {\n\t\tMap<String, Object> body = mapper.convertValue(principal, Map.class);\n"
                 "\t\tbody.put(\"clientId\", \"browser\");\n\t\treturn body;\n"),
                (f"{AUT}/controller/UserController.java", "\tprivate UserService userService;\n",
                 "\tprivate UserService userService;\n\n\t@Autowired\n\tprivate ObjectMapper mapper;\n")],
        "NB2": [(f"{AUT}/controller/UserController.java", '\t@RequestMapping(method = RequestMethod.POST)\n',
                 '\t@RequestMapping(value = "/count", method = RequestMethod.GET)\n\tpublic long countUsers() {\n'
                 '\t\treturn userService.count();\n\t}\n\n\t@RequestMapping(method = RequestMethod.POST)\n'),
                (f"{AUT}/service/UserService.java", "\tvoid create(User user);\n", "\tvoid create(User user);\n\n\tlong count();\n")],
    },
}


def git(cwd, *args):
    return subprocess.run(["git", "-C", str(cwd), *args], capture_output=True, check=True).stdout


def main():
    for (service, root), muts in MUTANTS.items():
        rel_args = [f"--relative=validation/provider-verification/piggymetrics"] if root == PIGGY else []
        git_root = REPO if root == PIGGY else PETCLINIC
        for mid, edits in muts.items():
            touched = sorted({f for f, _, _ in edits})
            try:
                for f, anchor, repl in edits:
                    p = root / f
                    raw = p.read_bytes().decode("utf-8")
                    crlf = "\r\n" in raw
                    text = raw.replace("\r\n", "\n")
                    if text.count(anchor) != 1:
                        sys.exit(f"{service} {mid}: ancla no única ({text.count(anchor)}) en {f}: {anchor!r}")
                    text = text.replace(anchor, repl)
                    p.write_bytes((text.replace("\n", "\r\n") if crlf else text).encode("utf-8"))
                paths = [str((root / f).relative_to(git_root)).replace("\\", "/") for f in touched]
                patch = git(git_root, "diff", *rel_args, "--", *paths)
                if not patch:
                    sys.exit(f"{service} {mid}: diff vacío")
                out = OUT / service / f"{mid}.patch"
                out.parent.mkdir(parents=True, exist_ok=True)
                out.write_bytes(patch)
                print(f"{service}/{mid}.patch  ({len(patch.splitlines())} líneas)")
            finally:
                paths = [str((root / f).relative_to(git_root)).replace("\\", "/") for f in touched]
                git(git_root, "checkout", "--", *paths)


if __name__ == "__main__":
    main()
