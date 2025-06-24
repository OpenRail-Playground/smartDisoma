package org.acme.employeescheduling.rest;

import java.util.Collection;
import java.util.List;

public class StaticDataProvider {

    public static Collection<String> getResourceCategories() {
        return List.of(
            "Arbeitsleiter/in",
            "Arbeitsstellenkoordinator/in",
            "Basistechniker/in",
            "Basistechniker/in SAI",
            "Begleiter/in",
            "Chefmonteur/in",
            "Chefmonteur/in BST",
            "Chefmonteur/in SAA",
            "Cheftechniker/in",
            "Cheftechniker/in SAI",
            "Facharbeiter/in extern",
            "Geomatiker/in",
            "Gleismonteur/in",
            "Gleismonteur/in komplex",
            "Handwerker/in",
            "Handwerker/in SAA",
            "Hilfsarbeiter/in extern",
            "Kranführer/in",
            "Lernende/r",
            "Monteur/in",
            "Monteur/in KLU SAA",
            "Polier/in",
            "Sicherheitschef/in",
            "Sicherheitsleiter/in",
            "Sicherheitswärter/in",
            "Spezialhandwerker/in",
            "Spezialmonteur/in",
            "Spezialmonteur/in BST",
            "Spezialmonteur/in SAA",
            "Spitzenlast temporär",
            "TFF",
            "Teamleiter/in",
            "Techniker/in",
            "Techniker/in SAI",
            "Techniker/in spezial",
            "Techniker/in spezial SAI",
            "Verdrahter/in extern",
            "Vermessungsassistent/in",
            "Vorarbeiter/in"
        );
    }

    public static Collection<String> getQualifications() {
        return List.of(
            "Bauleiter",
            "Güterzugsmodul",
            "Quereinsteiger",
            "Am 843 SBB",
            "Gefahrstoff INFRA WK",
            "Triebfahrzeugführende A / PP",
            "XTm Bendini",
            "Rangierbegleitende Ai40 / PP",
            "Selbstschutz Arbeiten WK",
            "Triebfahrzeugführende Art. 10b / PP",
            "Italienisch",
            "Sicherheitschef/in WK",
            "Zugbegleitende Bi / PP",
            "Tmf 232",
            "Fallschutz Höhensicherung",
            "Gefahrgut WK TFF und Begleiter Bau",
            "Kirow 100",
            "Triebfahrzeugführende B100 / PP",
            "Tm 232 Cargo",
            "Schienentransporteinheit elektrisch",
            "Arbeitsstellenkoordinator",
            "Tm 234",
            "Aem 940",
            "Zertifizierung Neutralisation",
            "Sicherheitswärter/in WK",
            "Tm 232",
            "Rangierbegleitende Ai / PP",
            "Niederflurwagen Xaas (NT)",
            "SUVA Kranausbildung Kat. A WK",
            "Am 841",
            "Instruktion TUBEMO",
            "Tm 234-4",
            "Schalten und Erden WK",
            "Schienentransporteinheit mechanisch",
            "ETCS L1",
            "Netz SBB",
            "Minimel Lynx Bediener",
            "Triebfahrzeugführende A40 / PP",
            "Arb. mit Motorsägen Kanthölzern MOSÄ1",
            "ETCS",
            "Instruierter Mitarbeiter RSS",
            "Anschlagen von Lasten"
        );
    }
}
