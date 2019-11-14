
# Entwicklung einer Android-App zur Vermessung und Visualisierung von WLAN-Empangsstärken

Bachelorarbeit an der Hochschule für Technik und Wirtschaft Berlin
Author: Emil Schoenawa

1. Prüfer: Prof. Dr.-Ing. Thomas Schwotzer
2. Prüfer: Dipl.-Ing. Jens Renner (AVM GmbH)

## Zielsetzung
In dieser Arbeit soll eine prototypische Android-App entstehen, die das Ausmessen von privaten WLAN-Netzen sowie das Visualisieren der gemessenen Empfangsfeldstärken erlaubt. Hierfür soll nur an
einzelnen Punkten die WLAN-Empfangsfeldstärke gemessen werden. Die fehlenden Empfangsfeldstärken sollen dann durch Interpolation bestimmt werden um eine flächendeckende Analyse zu erlauben.
Die Position von WLAN-Messungen soll in der App nur durch das Smartphone und ohne Nutzereingabe bestimmt werden. Dadurch wird die Bedienbarkeit der App deutlich erleichtert, da der
Nutzer nicht seine eigene Position auf einer Karte oder einem Grundriss fur jede Messung markieren muss. Die automatische Positionierung soll mithilfe von dem Augmented-Reality-Framework
ARCore von Google realisiert werden. Mithilfe dieses Frameworks kann auch eine Visualisierung ermöglicht werden, welche Schwachstellen des WLAN-Empfangs durch virtuelle Objekte
kennzeichnet.

*Weitere Informationen zu dem Projekt stehen in der Arbeit selbst im Ordner 'doc'*

## Inhalt dieses Repositories
In diesem Repository befindet sich der digitale Anhang der Bachelorarbeit.
Folgende Verzeichnisse sind vorhanden:

* apps: Enthält die Android-Studio-Projekte der App "WifiAR" zum
    Vermessen und Visualisieren von WLAN-Empfangsstärken und
    der App "RTTDemo" zum Testen der Funktionalität der im
    WLAN-Standard 802.11mc spezifizierten Abstandsmessung mit dem
    Round-Time-Trip-Verfahren. In diesem Verzeichnis befinden sich
    ebenfalls signierte .apk-Dateien für beide Apps.

    HINWEIS: Die App "WifiAR" benötigt ein mit dem Framework ARCore
    kompatibles Smartphone. Die App "ARCore" muss ebenfalls aus dem
    Google Play Store heruntergeladen werden bevor die App korrekt
    funktioniert.
    Die App "RTTDemo" erfordert ein Smartphone mit mindestens
    Android 9 (Android P, API-Level 28) und hardwareseitiger
    Unterstützung von 802.11mc.
* latex_project: LaTeX-Quellcode der Arbeit
* doc: In diesem Verzeichnis befindet sich eine digitale Version (PDF-
    Format) der Bachelorarbeit sowie ein Ordner mit den in der
    Arbeit verwendeten Bildern.

* videos
    Enthält drei Videos, in denen die Funktionalität der App "WifiAR"
    mit unterschiedlichen Einstellungen demonstriert wird.
## Hilfreiche Links
ARCore-kompatible Geräte:
 https://developers.google.com/ar/discover/supported-devices

ARCore App im Google Play Store:
 https://play.google.com/store/apps/details?id=com.google.ar.core

