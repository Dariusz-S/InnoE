import DataFiles.Company;
import DataFiles.Event;
import DataFiles.ListedCompany;
import org.apache.groovy.util.ReversedList;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;

import static io.restassured.RestAssured.given;

public class CompareDataV2 extends Setup {

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
    public void calculatedIEEquityPercFromEventsShoudBeEqualToIEEquityFromCompanyLevel() {
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
        List <ListedCompany> listedCompaniesDetails = Arrays.stream(listedCompanies).toList();
        //--------------------------------------------------------------------------------------------------------

        listedCompaniesDetails.forEach(listedCompany -> {
           // System.out.println(listedCompany.getCompanyID());

        });
        companyEvents.forEach((c, e) -> {
            double innoEnergyShareholdingFlat = e.stream().mapToDouble(event -> event.innoEnergyShareholdingFlat).sum();
            double totalShareholdingFlat = e.stream().mapToDouble(event -> event.totalShareholdingFlat).sum();
            boolean found = listedCompaniesDetails.stream()
                    .anyMatch(listedCompany1 -> Objects.equals(listedCompany1.getCompanyID(), Double.valueOf(c.getCompanyID())));
            if (found) {
                System.out.println(c.companyID+ " To jest listed z stremowania");
            }

            Double calculatedIEShareholdingPercentage = innoEnergyShareholdingFlat * 100 / totalShareholdingFlat;
           // System.out.println("ID z c: "+Double.valueOf(c.getCompanyID()));


           // System.out.println("ID z listed Details:" + listedCompaniesDetails.get(0).getCompanyID());
            //listedCompaniesDetails.forEach(listedCompany -> {
             //   if (listedCompany.getCompanyID().equals(Double.valueOf(c.getCompanyID()))){
            //        System.out.println(listedCompany.getCompanyID() + "  To jest Listed");
            //    }
           // });

/*----------------------------
            DecimalFormat decimalFormat = new DecimalFormat("#.##");
            decimalFormat.setRoundingMode(RoundingMode.DOWN);

            // softly.assertThat(1.0).isCloseTo(0.98, Offset.offset(0.01));

//            softly.assertThat(decimalFormat.format(calculatedIEShareholdingPercentage))
//                    .as("calculatedIEShareholdingPercentage company id: " + c.companyID)
//                    .isEqualTo(decimalFormat.format(c.ieEquityPercentage));
            Optional<Event> maxEventDate = e.stream().max(Comparator.comparing(Event::getEventDate)); //to find event with max date
            long count = e.stream().filter(e1 -> e1.eventDate.equals(maxEventDate.get().eventDate)).count();


            // define the fields by which to sort
            Comparator<Event> comparator = Comparator.comparing(event -> event.eventDate);
            comparator = comparator.thenComparing(event -> event.eventSequenceCalc).reversed();

            // sort e list and assigned to new list
            final List<Event> events1 = e.stream()
                    .sorted(comparator)
                    .toList();
            ;
            // get first event from sorted list (with max date and max seq)
            System.out.println("MMMMMMMMMMMMMMMMMMMMM" + events1.get(0));
            System.out.println("Max date from company: " + maxEventDate.get().companyID + "  event date:" + maxEventDate.get().eventDate + "  count" + count);

        ----------------------------------*/
        });
    }
}



