package com.webscraper.Scraper.service;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.springframework.stereotype.Service;

@Service
public class ColorService {

    private String getCorrectColorFromWebElement(WebElement element) {
        WebElement childColorInfo = null;

        if(element.findElements(By.tagName("font")).size() > 0) {
            childColorInfo = element.findElement(By.tagName("font"));
        }
        if(!(childColorInfo == null)) {
            if(childColorInfo.findElements(By.tagName("font")).size() > 0) {
                WebElement grandChildColorInfo = childColorInfo.findElement(By.tagName("font"));
                if(!(grandChildColorInfo == null)) {
                    return grandChildColorInfo.getCssValue("color");
                }
            }
            return childColorInfo.getCssValue("color");
        } else {
            return element.getCssValue("color");
        }
    }

    private String convertRgbColorToString (String rgbColor) {
        if(rgbColor.equals("rgba(0, 153, 102, 1)") || rgbColor.equals("rgba(0, 153, 51, 1)")) {
            return "zelena";
        }
        if(rgbColor.equals("rgba(255, 0, 0, 1)")) {
            return "crvena";
        }
        if(rgbColor.equals("rgba(255, 153, 0, 1)")) {
            return "zuta";
        }
        return "unknownColor";
    }

    public String getColor(WebElement webElement) {
        String rgbColor = getCorrectColorFromWebElement(webElement);
        return convertRgbColorToString(rgbColor);
    }


}
