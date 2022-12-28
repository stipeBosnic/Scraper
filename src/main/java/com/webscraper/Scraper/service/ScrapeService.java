package com.webscraper.Scraper.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.*;
import java.util.List;

import static java.lang.Thread.sleep;

@Service
@Slf4j
@RequiredArgsConstructor
public class ScrapeService {

    private static final String URL = "https://e-brojevi.udd.hr/e_brojevi.htm";

    private final ChromeDriver chromeDriver;

    private final ColorService colorService;

    private final CustomHelpService customHelpService;

    @Value("${headerForAdditiveTypeCsv}")
    private String headerForAdditiveTypeCsv;

    @Value("${headerForAdditiveCsv}")
    private String headerForAdditiveCsv;

    @Value("${aditivCsvPath}")
    private String aditivCsvPath;

    @Value("${antioksidansiCsvPath}")
    private String antioksidansiCsvPath;

    @Value("${bojilaCsvPath}")
    private String bojilaCsvPath;

    @Value("${konzervansiCsvPath}")
    private String konzervansiCsvPath;

    @Value("${pojacivaciOkusaCsvPath}")
    private String pojacivaciOkusaCsvPath;

    @Value("${regulatoriKiselostiCsvPath}")
    private String regulatoriKiselostiCsvPath;

    @Value("${stabilizatoriCsvPath}")
    private String stabilizatoriCsvPath;

    @Value("${umjetnaSladilaCsvPath}")
    private String umjetnaSladilaCsvPath;



    @PostConstruct
    void start() throws InterruptedException, FileNotFoundException {
        scrapeAndWrite();
    }
    public void scrapeAndWrite() throws InterruptedException, FileNotFoundException {
        final PrintWriter printWriterForAditiv = new PrintWriter(aditivCsvPath);
        final PrintWriter printWriterForBojila = new PrintWriter(bojilaCsvPath);
        final PrintWriter printWriterForKonzervansi = new PrintWriter(konzervansiCsvPath);
        final PrintWriter printWriterForAntioksidansi = new PrintWriter(antioksidansiCsvPath);
        final PrintWriter printWriterForStabilizatori = new PrintWriter(stabilizatoriCsvPath);
        final PrintWriter printWriterForRegulatoriKiselosti = new PrintWriter(regulatoriKiselostiCsvPath);
        final PrintWriter printWriterForPojacivaciOkusa = new PrintWriter(pojacivaciOkusaCsvPath);
        final PrintWriter printWriterForUmjetnaSladila = new PrintWriter(umjetnaSladilaCsvPath);
        printWriterForAditiv.append(headerForAdditiveCsv);
        printWriterForBojila.append(headerForAdditiveTypeCsv);
        printWriterForKonzervansi.append(headerForAdditiveTypeCsv);
        printWriterForAntioksidansi.append(headerForAdditiveTypeCsv);
        printWriterForStabilizatori.append(headerForAdditiveTypeCsv);
        printWriterForRegulatoriKiselosti.append(headerForAdditiveTypeCsv);
        printWriterForPojacivaciOkusa.append(headerForAdditiveTypeCsv);
        printWriterForUmjetnaSladila.append(headerForAdditiveTypeCsv);

        chromeDriver.get(URL);
        sleep(3000);
        List<WebElement> anchors = chromeDriver.findElements(By.tagName("a"));
        List<String> listOfAllLinks = anchors.stream().map(element -> element.getAttribute("href")).toList();
        List<String> httpsLinks = customHelpService.getLinksThatStartWithHttps(listOfAllLinks);
        final int[] i = {1};
        httpsLinks.forEach(link -> {
            chromeDriver.get(link);

            List<WebElement> allTitles = chromeDriver.findElements(By.tagName("b"));

            Optional<String> additiveTitle = allTitles.stream().filter(webElement -> webElement.getText().startsWith("E"))
                    .map(additive -> additive.getText()).findFirst();

            if(additiveTitle.isEmpty()) {
                allTitles = chromeDriver.findElements(By.tagName("font"));
                additiveTitle = customHelpService.fixInvalidAdditiveTitles(additiveTitle);
            }

            Optional<WebElement> colorInfo = allTitles.stream().filter(webElement -> webElement.getText().startsWith("E")).findFirst();

            if (additiveTitle.isPresent()) {

                if (additiveTitle.get().length() < 6) {
                    additiveTitle = customHelpService.fixShortAdditiveTitles(additiveTitle, allTitles);
                }

                String additiveTitleForCsv = customHelpService.convertToCsvFormat(additiveTitle.get());
                if (additiveTitleForCsv.contains("E621") || additiveTitleForCsv.contains("E541")) {
                    additiveTitleForCsv = new StringBuilder(additiveTitleForCsv).insert(4, " ").toString();
                }

                List<String> additiveInfo = new ArrayList<>(Arrays.stream(additiveTitleForCsv.split(" ", 2)).toList());

                String additiveType = customHelpService.getAdditiveTypeFromTitle(additiveInfo.get(0)).replaceAll(",", "|");
                additiveInfo.add(additiveType);

                if (additiveInfo.get(0).startsWith("E436")) {
                    additiveInfo.add("zuta");
                } else if (additiveInfo.get(0).startsWith("E585")) {
                    additiveInfo.add("zelena");
                } else {
                    String color = colorService.getColor(colorInfo.get());
                    additiveInfo.add(color);
                }

                List<WebElement> allImages = chromeDriver.findElements(By.tagName("img"));
                Optional<WebElement> elementImage = allImages.stream().filter(tag -> tag.getAttribute("src").contains("e_strukture")).findFirst();
                if(elementImage.isPresent()) {
                    String imageURL = elementImage.get().getAttribute("src");
                    try {
                        BufferedImage image = ImageIO.read(new URL(imageURL));
                        File file = new File("src/main/resources/images/" + additiveInfo.get(0) + ".png");
                        ImageIO.write(image, "png", file);
                        additiveInfo.add(additiveInfo.get(0) + ".png");
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    additiveInfo.add("no image");
                }

                List<WebElement> allText = chromeDriver.findElements(By.tagName("body"));
                String description = customHelpService.getDescriptionOfAnAdditive(allText);
                additiveInfo.add(description);

                //Ispisi trenutacni element u aditiv.csv file
                customHelpService.printListToCsv(additiveInfo, printWriterForAditiv);


                //Ispisi trenutacni element u odgovarajuci file za tip aditiva
                List<String> additive = List.of(additiveInfo.get(0), additiveInfo.get(1));
                if(additiveInfo.get(2).startsWith("bojilo")) {
                    customHelpService.printListToCsv(additive, printWriterForBojila);
                } if(additiveInfo.get(2).startsWith("konzervans")) {
                    customHelpService.printListToCsv(additive, printWriterForKonzervansi);
                } if(additiveInfo.get(2).startsWith("antioksidans")) {
                    customHelpService.printListToCsv(additive, printWriterForAntioksidansi);
                } if(additiveInfo.get(2).startsWith("stabilizator")) {
                    customHelpService.printListToCsv(additive, printWriterForStabilizatori);
                } if(additiveInfo.get(2).startsWith("regulator")) {
                    customHelpService.printListToCsv(additive, printWriterForRegulatoriKiselosti);
                } if(additiveInfo.get(2).startsWith("pojačivač")) {
                    customHelpService.printListToCsv(additive, printWriterForPojacivaciOkusa);
                } if(additiveInfo.get(2).startsWith("umjetno")) {
                    customHelpService.printListToCsv(additive, printWriterForUmjetnaSladila);
                }
            }
        });
        printWriterForAditiv.close();
        printWriterForBojila.close();
        printWriterForKonzervansi.close();
        printWriterForAntioksidansi.close();
        printWriterForPojacivaciOkusa.close();
        printWriterForRegulatoriKiselosti.close();
        printWriterForStabilizatori.close();
        printWriterForUmjetnaSladila.close();

        chromeDriver.close();
    }
}


