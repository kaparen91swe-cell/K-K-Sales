# K-K-Sales
Economy program for booking
Här är en praktisk, systematisk steg-för-steg-guide som tar dig från noll till en fungerande, säker Flutter-app (K&K Sales) med lokal lagring, admin-åtkomst, CI och förberedelse för framtida synchronisering. Jag delar upp det i vilka verktyg du behöver, vilka filer du ska skapa/uppdatera, och vilka kommandon du kör. Jag inkluderar också hur du hanterar admin-lösenord i Secure Storage och hur du skapar en GitHub PR mot ditt privata repo.

Översikt av verktyg och miljö
Datorverktyg
  Flutter SDK
  Android Studio eller Visual Studio Code (VS Code)
  Git (klon/commit/push)
  (valfritt) Xcode (om du vill bygga iOS via macOS)
Tjänst
  GitHub-konto med ditt privata repo: https://github.com/kaparen91swe-cell/K-K-Sales.git
Paket och beroenden i projektet
  Flutter-paket: sqflite, path_provider, flutter_secure_storage, csv, json_annotation, riverpod (eller provider)
  Firebase/Supabase (valfritt senare för synkronisering)
CI/CD
  GitHub Actions (körs automatiskt när du pushar eller öppnar PR)

Steg 1: Förbered din utvecklingsmiljö
1. Installera Flutter
  Följ installationsguiderna: https://flutter.dev/docs/get-started/install
  Verifiera installationen:
    flutter doctor
2. Installera en IDE
  VS Code: https://code.visualstudio.com/
  eller Android Studio: https://developer.android.com/studio
  Installera Flutter- och Dart-plugins i din valda IDE
3. Installera Git
  https://git-scm.com/downloads
  Konfigurera användare: git config user.name, git config user.email

Steg 2: Förbered projektmapp och filer lokalt
1) Skapa en ny tom Flutter-projektstruktur (om du vill börja från scratch)
  Alternativt klona ditt befintliga repo och arbeta direkt där.
  Klona ditt privata repo:
    git clone https://github.com/kaparen91swe-cell/K-K-Sales.git
    cd K-K-Sales
2) Lägg till eller skapa filer enligt den exakt filstruktur vi specificerat:
  .github/workflows/flutter.yml
  android/
  ios/
  lib/
    main.dart
    models/user.dart (med roll)
    models/product.dart
    models/sale.dart
    db/database.dart
    screens/ (products_screen.dart, users_screen.dart, sales_screen.dart, history_screen.dart)
    widgets/ (input_field.dart, item_card.dart)
    services/ (security.dart, json_import_export.dart, csv_export.dart)
  data/sample_data.json
  pubspec.yaml
  README.md
3) Uppdatera pubspec.yaml för att inkludera nödvändiga paket (om du inte redan gjort)
  flutter_secure_storage: ^5.0.0
  sqflite: ^2.0.0+4
  path_provider: ^2.0.11
  csv: ^5.0.0
  json_annotation: ^4.8.0
  flutter_riverpod: ^2.0.0 (eller provider om du föredrar)
4) Kör i projektets rot:
  flutter pub get

Steg 3: Implementera admin-säkerhet och rollen i koden
1) Lägg till roll i användarmodell
  Se lib/models/user.dart i vårt tidigare exempel (roll: 'admin' eller 'user')
2) Lösenordshantering i Secure Storage
  Lägg till lib/services/security.dart som hanterar admin-lösenord med flutter_secure_storage
  Implementera metoder:
    isAdminSetup()
    setAdminPassword(String password)
    verifyAdminPassword(String password)
3) Initiera admin-lösenordet vid första körningen
  I AdminSetupScreen/AdminAuthWrapper i lib/main.dart, använd SecurityService för att skapa och verifiera lösenord

Steg 4: UI och admin-flöde
1) Anpassa lib/main.dart så att appen startar i admin-läget (AdminAuthWrapper) eller visar AdminSetupScreen om admin-lösenord saknas
2) Se till att admin-flödet har tillgång till skapa nya användare och produkter, samt hantera restriktioner
3) Lägg till vyer i lib/screens/* som visar/begränsar funktioner beroende på currentUser.role

Steg 5: Datamodeller och DB-lagring
1) Uppdatera lib/db/database.dart så att User-tabellen har fältet role
2) Se till att CRUD-operationer fungerar med rollen
3) Uppdatera datamodellerna (User, Product, Sale) för att stödja roll och säkerhetsaspekter
4) Stöd för import/export (JSON/CSV) via lib/services/json_import_export.dart och lib/services/csv_export.dart

Steg 6: CI och testkonfiguration
1) Lägg till eller uppdatera .github/workflows/flutter.yml:
  Kör flutter pub get
  Flutter analyze
  Flutter test
2) Om du vill lägga till build av APK i CI (utan signing) kan du utöka workflow med:
  flutter build apk --debug
  (Release-bygg kräver signing; du kan lägga till steg senare när du konfigurerar signing i GitHub Secrets)
3) Om du vill lägga till lints eller extra tester kan du lägga till ytterligare steg i workflow

Steg 7: Bygga APK och köra lokalt
1) Förbered signing lokalt (om du vill skapa release APK)
  Skapa keystore: keytool -genkey -v -keystore my-release-key.keystore -alias my-key-alias -keyalg RSA -keysize 2048 -validity 10000
  Lägg till signingConfigs i android/app/build.gradle
2) Bygg APK-release
  flutter build apk --release
  Hitta filen: build/app/outputs/flutter-apk/app-release.apk

Steg 8: Dataimport/export och initialdata
1) Importera initialdata:
  I admin-vyn, tryck Importera initialdata (knapp för JSON-import)
  Eller använd lib/services/json_import_export.dart: importFromAsset() för att läsa data/sample_data.json
2) Exportera data:
  JSON: exportToJsonString()
  CSV: exportAllSalesToCsv()

Steg 9: Hur du gör en PR till ditt privata repo
1) Skapa en ny branch i ditt privata repo (exempelvis feature/security-ci)
  git checkout -b feature/security-ci
2) Lägg till de nya/uppdaterade filerna i repo
  git add .
  git commit -m "Add local admin security with Secure Storage, admin roles, and CI workflow"
3) Pusha branch till ditt privata repo
  git push origin feature/security-ci
4) Öppna GitHub, skapa en Pull Request mot main/mast
5) När PR granskas, lintas och CI körs automatiskt i bakgrunden

Steg 10: Nästa steg och förbättringar
Efter att du har MVP-koden och CI i plats kan vi lägga till:
  Firebase/ Supabase för realtids-synk när fler än en användare jobbar
  Bättre autentisering (användare kan logga in med lokal auth i appen; admin har högre privilegier)
  Konflikthantering i synk
  Säker inloggning och lösenordsåterställning
  UI-önskemål: bättre design, felmeddelanden, validering

Nästkommenderande steg för dig
Välj hur du vill skapa PR: A) jag genererar en färdig diff/patch här som du kan spara och ladda upp som PR, B) jag levererar en tydlig PR-beskrivning och en sammanhängande patch att klistra in i PR-chat (du kan använda GitHub UI för att skapa PR mot ditt privata repo).
Om du vill att jag genererar diff/patch nu, säg vilka filer du vill få som patches först (t.ex. lib/models/user.dart, lib/db/database.dart, lib/services/security.dart, lib/main.dart, .github/workflows/flutter.yml, pubspec.yaml, README.md). Jag levererar då en sammanhängande diff-block som du kan spara som patch-fil och köra git apply.

Vill du att jag går vidare med att generera den färdiga diff/patchen och den uppdaterade PR-beskrivningen nu?
