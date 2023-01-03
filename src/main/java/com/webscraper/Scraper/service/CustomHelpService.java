package com.webscraper.Scraper.service;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.springframework.stereotype.Service;

import java.io.PrintWriter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomHelpService {

    private final ChromeDriver chromeDriver;

    public String getDescriptionOfAnAdditive(List<WebElement> allText) {
        StringBuilder descriptionBuilder = new StringBuilder();
        allText.stream().map(webElement -> webElement.getText()).forEach(textFromElement -> descriptionBuilder.append(textFromElement));
        String description = descriptionBuilder.toString();
        String descriptionForCsv = description.replaceAll(",", ";");
        descriptionForCsv = descriptionForCsv.replace("\n", ";").replace("\r", "");
        return descriptionForCsv;
    }

    public Optional<String> fixShortAdditiveTitles(Optional<String> additiveTitle, List<WebElement> allTitles) {
        WebElement additiveTitleSecondPart = null;
        if (allTitles.stream().toList().size() > 1) {
            additiveTitleSecondPart = allTitles.stream().toList().get(1);
        } else {
            additiveTitleSecondPart = chromeDriver.findElements(By.tagName("strong"))
                    .stream().findFirst().get();
        }
        String additiveTitleSecondPartText = additiveTitleSecondPart.getText();
        return additiveTitle = Optional.of(additiveTitle.get() + " " + additiveTitleSecondPartText);
    }

    public Optional<String> fixInvalidAdditiveTitles(Optional<String> additiveTitle) {
        List<WebElement> allTitles = chromeDriver.findElements(By.tagName("font"));
        if (additiveTitle.isEmpty()) {
            additiveTitle = allTitles.stream().filter(webElement -> webElement.getText().startsWith("E3"))
                    .map(additive -> additive.getText()).findFirst();
        }
        if (additiveTitle.isEmpty()) {
            additiveTitle = allTitles.stream().filter(webElement -> webElement.getText().startsWith("436"))
                    .map(additive -> additive.getText()).findFirst();
            if (additiveTitle.isPresent()) {
                additiveTitle = Optional.of("E" + additiveTitle.get());
            }
        }
        if (additiveTitle.isEmpty()) {
            additiveTitle = allTitles.stream().filter(webElement -> webElement.getText().startsWith("Želj"))
                    .map(additive -> additive.getText()).findFirst();
            if (additiveTitle.isPresent()) {
                additiveTitle = Optional.of("E585 Željezov (II) laktat");
            }
        }
        return additiveTitle;
    }

    public List<String> getLinksThatStartWithHttps(List<String> listOfLinks) {
        return listOfLinks.stream().filter(link -> link.startsWith("https")).collect(Collectors.toList());
    }

    public String convertToCsvFormat(String additiveTitle) {
        String additiveTitleForCsv = additiveTitle.replaceAll(",", ";");
        if (additiveTitleForCsv.length() > 3) {
            if (additiveTitleForCsv.substring(0, 4).contains(" ")) {
                return StringUtils.replaceOnce(additiveTitleForCsv, " ", "");
            }
        }
        return additiveTitleForCsv;
    }

    public String getAdditiveTypeFromTitle(String title) {

        if (title.startsWith("E1") && title.length() == 4 || (title.startsWith("E1") && title.substring(1).matches(".*[a-zA-Z]+.*"))) {
            return "bojilo";
        }
        if (title.startsWith("E2")) {
            return "konzervans";
        }
        if (title.startsWith("E3")) {
            return "antioksidans, regulator kiselosti...";
        }
        if (title.startsWith("E4")) {
            return "stabilizator, emulgator, zgušnjivač, tvar za zadržavanje vlage kiselosti";
        }
        if (title.startsWith("E5")) {
            return "regulator kiselosti, tvar za sprječavanje zgrudnjavanja";
        }
        if (title.startsWith("E6")) {
            return "pojačivač okusa";
        }
        if (title.startsWith("E9")) {
            return "umjetno sladilo, stabilizator, zgušnjivač";
        }
        if (title.startsWith("E1") && title.length() == 5 && !title.substring(1).matches("[A-Za-z]+")) {
            return "umjetno sladilo, stabilizator, zgušnjivač";
        }
        return "unknown";
    }

    public void printListToCsv(List<String> additiveInfo, PrintWriter printWriter) {
        String printCsv = String.join(",", additiveInfo);
        if (!printCsv.contains("unknown")) {
            printWriter.append(printCsv);
            printWriter.append("\n");
        }
    }
}
