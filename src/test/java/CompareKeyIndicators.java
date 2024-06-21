import DataFiles.Company;
import DataFiles.Event;
import DataFiles.ListedCompany;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;

import static io.restassured.RestAssured.given;

public class CompareKeyIndicators extends Setup {
    private static Company[] companies;
    private static Event[] events;
    private static ListedCompany[] listedCompanies;

    @BeforeAll
    public static void getData() {
        companies = given()
                .spec(companyDetailsEndpoint)
                .when()
                .get()
                .then()
                .assertThat()
                .statusCode(200)
                .extract()
                .as(Company[].class);

        events = given()
                .spec(eventDetailsEndpoint)
                .when()
                .get()
                .then()
                .assertThat()
                .statusCode(200)
                .extract()
                .as(Event[].class);

        listedCompanies = given()
                .spec(listedCompanyDetailsEndpoint)
                .when()
                .get()
                .then()
                .assertThat()
                .statusCode(200)
                .extract()
                .as(ListedCompany[].class);


    }


    @Test
    public void calculatedIEEquityPercFromEventsShouldBeEqualToIEEquityFromCompanyLevel() {
        Map<Company, List<Event>> companyEvents = new HashMap<>();

        List<Company> shareholdingCompanies = Arrays.stream(companies)
                .filter(company -> company.equityStatus.equals("Shareholding")).toList();

        shareholdingCompanies.forEach(company -> {
            List<Event> eventList = Arrays.stream(events)
                    .filter(event -> event.companyID.equals(Double.valueOf(company.companyID)))
                    .filter(event -> event.eventStatus.equals("Approved") || event.eventStatus.equals("Completed"))
                    .collect(Collectors.toList());
            companyEvents.put(company, eventList);
        });

        List<ListedCompany> listedCompaniesDetails = Arrays.stream(listedCompanies).toList();
        //--------------------------------------------------------------------------------------------------------


        companyEvents.forEach((c, e) -> {
            Double totalShareholding;
            Double innoEnergyShareholdingFlat = e.stream().mapToDouble(event -> event.innoEnergyShareholdingFlat).sum();

            boolean found = listedCompaniesDetails.stream()
                    .anyMatch(listedCompany1 -> Objects.equals(listedCompany1.getCompanyID(), Double.valueOf(c.getCompanyID())));

            if (found) {
                Comparator<ListedCompany> comparator = Comparator.comparing(entry -> entry.validTo);
                comparator = comparator.reversed();
                List<ListedCompany> listedTransactions = listedCompaniesDetails.stream()
                        .filter(compId -> compId.companyID.equals(Double.valueOf(c.companyID)))
                        .sorted(comparator)
                        .collect(Collectors.toList());
                totalShareholding = listedTransactions.get(0).listedNumberOfShares;
            } else {
                totalShareholding = e.stream().mapToDouble(event -> event.totalShareholdingFlat).sum();
            }

            Double calculatedIEShareholdingPercentage = innoEnergyShareholdingFlat * 100 / totalShareholding;

            if (calculatedIEShareholdingPercentage.isNaN()) {
                calculatedIEShareholdingPercentage = 0.0;
            }

            DecimalFormat decimalFormat = new DecimalFormat("#.##");
            decimalFormat.setRoundingMode(RoundingMode.DOWN);

            softly.assertThat(calculatedIEShareholdingPercentage)
                    .as(c.companyID + " something went wrong  " + "total shares: " + totalShareholding + "  IE shares: " + innoEnergyShareholdingFlat)
                    .isCloseTo(c.ieEquityPercentage, Offset.offset(0.01));

/*
           softly.assertThat(decimalFormat.format(calculatedIEShareholdingPercentage))
                   .as("calculatedIEShareholdingPercentage company id: " + c.companyID)
                   .isEqualTo(decimalFormat.format(c.ieEquityPercentage));
*/
        });
    }


}
