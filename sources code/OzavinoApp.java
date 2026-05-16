import javax.swing.*;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.net.URI;
import java.util.*;
import java.util.List;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

public class OzavinoApp extends JFrame {

    // ======================= THÈME CENTRALISÉ =======================
    // Toutes les couleurs et polices en un seul endroit — modifier ici change tout.
    static final Color FOND       = new Color(240, 235, 220);
    static final Color CIEL       = new Color(26,  60,  94);
    static final Color ACACIA     = new Color(212, 163, 115);
    static final Color ARGILE     = new Color(204, 90,  32);
    static final Color NUIT       = new Color(13,  27,  42);
    static final Color KOLA       = new Color(92,  58,  33);
    static final Color TEXTE_CLAIR= new Color(245, 230, 211);
    static final Color TEXTE_FONCE= new Color(50,  35,  25);
    static final Color ROSEHIP    = new Color(121, 35,  24);
    static final Color VERT_OK    = new Color(100, 200, 100);

    static final Font FONT_TITRE   = new Font("Serif", Font.BOLD,  22);
    static final Font FONT_SECTION = new Font("Serif", Font.BOLD,  18);
    static final Font FONT_BODY    = new Font("Serif", Font.PLAIN, 14);
    static final Font FONT_SMALL   = new Font("Serif", Font.PLAIN, 13);
    static final Font FONT_BTN     = new Font("Serif", Font.BOLD,  12);

    // ======================= ÉTAT DE L'APPLICATION =======================
    // Alias pour compatibilité avec le code existant
    Color fond        = FOND;
    Color ciel        = CIEL;
    Color acacia      = ACACIA;
    Color argile      = ARGILE;
    Color nuit        = NUIT;
    Color kola        = KOLA;
    Color texteClair  = TEXTE_CLAIR;
    Color texteFonce  = TEXTE_FONCE;
    Color rosehip     = ROSEHIP;

    CardLayout cardLayout = new CardLayout();
    JPanel mainContent    = new JPanel(cardLayout);
    JLabel topBanner      = new JLabel();
    JLabel sideBanner     = new JLabel();
    JScrollPane scrollPane;

    // Navigation : mémorise la page actuelle pour le bouton Retour et la traduction
    private String pageActuelle = "Accueil";

    // Cache des pages statiques (évite la duplication dans le CardLayout)
    private final Map<String, JPanel> pageCache = new HashMap<>();

    // Traduction
    private final Map<String, String[]> dico = new HashMap<>();
    private String langueActuelle = "FR";

    // Connexion
    private boolean estConnecte  = false;
    private String  nomUtilisateur = "";

    // Données compte
    private ArrayList<String[]> lecturesEnCours;
    private ArrayList<String>   aLire;
    private ArrayList<String>   livresTermines;   // NOUVEAU : livres à 100%
    private ArrayList<String>   objectifs;
    private String userEmail;
    private String userInspirations;

    // Persistance
    private static final Preferences PREFS = Preferences.userNodeForPackage(OzavinoApp.class);

    // Catalogue
    private final Map<String, LivreDetail> catalogue = new HashMap<>();

    // ======================= CLASSE LIVREDETAIL =======================
    static class LivreDetail {
        String titre, auteur, date, genre, synopsis, statut, cheminCouverture;

        LivreDetail(String t, String a, String d, String g, String s, String st, String ch) {
            this.titre = t; this.auteur = a; this.date = d;
            this.genre = g; this.synopsis = s; this.statut = st;
            this.cheminCouverture = ch;
        }
    }

    // ======================= CONSTRUCTEUR =======================
    public OzavinoApp() {
        remplirDico();
        remplirCatalogue();
        chargerDonnees();      // Persistance : charge les données sauvegardées

        setTitle("OZAVINO - Bibliothèque Universelle");
        setSize(1200, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Sauvegarder à la fermeture
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { sauvegarderDonnees(); }
        });

        construireInterface();
    }

    // ======================= CONSTRUCTION DE L'INTERFACE =======================
    private void construireInterface() {
        // HEADER
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(nuit);
        topBanner.setHorizontalAlignment(SwingConstants.CENTER);
        header.add(topBanner, BorderLayout.CENTER);

        // MENU
        JPanel menuPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 30, 5));
        menuPanel.setBackground(nuit);
        menuPanel.add(createMenuBtn(t("accueil"),    "images/menu_recherches.png.jpg", "Accueil"));
        menuPanel.add(createMenuBtn(t("compte"),     "images/menu_compte.pnj.jpg",     "Compte"));
        menuPanel.add(createMenuBtn(t("categories"), "images/menu_categories.png.jpg", "Catégories"));

        // RECHERCHE — touche Entrée + vide le champ après recherche + multi-résultats
        JTextField searchField = new JTextField(15);
        searchField.setBackground(texteClair);
        searchField.setForeground(kola);
        searchField.setBorder(BorderFactory.createLineBorder(argile, 2));

        JButton searchBtn = new JButton("🔍");
        searchBtn.setContentAreaFilled(false);
        searchBtn.setBorderPainted(false);
        searchBtn.setForeground(texteClair);

        // Action de recherche — cherche dans tous les livres, affiche une liste si plusieurs résultats
        ActionListener searchAction = e -> {
            String query = searchField.getText().toLowerCase().trim();
            if (query.isEmpty()) return;

            List<LivreDetail> resultats = catalogue.values().stream()
                .filter(l -> l.titre.toLowerCase().contains(query)
                          || l.auteur.toLowerCase().contains(query)
                          || l.genre.toLowerCase().contains(query))
                .collect(Collectors.toList());

            searchField.setText("");  // Vide le champ après recherche
            searchField.requestFocus();

            if (resultats.isEmpty()) {
                afficherNotification(t("aucun_resultat") + " : " + query);
            } else if (resultats.size() == 1) {
                LivreDetail livre = resultats.get(0);
                afficherPageLecture(livre, "Accueil");
            } else {
                // Plusieurs résultats : afficher une page de liste avec les résultats
                afficherResultatsRecherche(query, resultats);
            }
        };
        searchBtn.addActionListener(searchAction);
        searchField.addActionListener(searchAction);  // Touche Entrée

        menuPanel.add(searchField);
        menuPanel.add(searchBtn);

        // LANGUES
        JLabel langLabel = new JLabel(t("langues") + " : ");
        langLabel.setForeground(texteClair);
        menuPanel.add(langLabel);
        String[] languages = {"FR", "EN", "ES"};
        JComboBox<String> langBox = new JComboBox<>(languages);
        // ItemListener déclenché même si on re-sélectionne la même valeur
        langBox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                langueActuelle = (String) langBox.getSelectedItem();
                traduireApplication();
            }
        });
        menuPanel.add(langBox);
        header.add(menuPanel, BorderLayout.SOUTH);

        // SCROLLPANE avec reset automatique de position à chaque navigation
        scrollPane = new JScrollPane(mainContent);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setBackground(fond);
        scrollPane.getViewport().setBackground(fond);
        styleScrollBar(scrollPane);

        setLayout(new BorderLayout());
        add(header, BorderLayout.NORTH);
        add(sideBanner, BorderLayout.WEST);
        add(scrollPane, BorderLayout.CENTER);

        // PAGES (cachées, créées une seule fois)
        rebuildStaticPages();
        showPage("Accueil");
    }

    /** Crée (ou recrée) les pages statiques et les met en cache. */
    private void rebuildStaticPages() {
        pageCache.clear();
        mainContent.removeAll();

        pageCache.put("Accueil",    createPageAccueil());
        pageCache.put("Catégories", createPageCategories());
        pageCache.put("Connexion",  createPageConnexion());
        pageCache.put("Compte",     createPageCompte());

        pageCache.forEach(mainContent::add);
    }

    private void showPage(String nom) {
        
        if ("Compte".equals(nom) && !estConnecte) {
            nom = "Connexion";
        }
        if ("Compte".equals(nom)) {
            refreshPageCompte();
        }
        String h = "images/ban_page1_horitentale.png.png";
        String v = "images/ban_page1_verticale.png.png";
        if ("Catégories".equals(nom) || nom.startsWith("Liste_")) {
            h = "images/ban_page3_horizontale.png.png";
            v = "images/ban_page3_verticale.png.jpg";
        } else if ("Compte".equals(nom) || "Connexion".equals(nom)) {
            h = "images/ban_page2_horizentale.png.png";
            v = "images/ban_page2_verticale.png.png";
        }
        try {
            topBanner.setIcon(new ImageIcon(new ImageIcon(h).getImage().getScaledInstance(800, 180, Image.SCALE_SMOOTH)));
            sideBanner.setIcon(new ImageIcon(new ImageIcon(v).getImage().getScaledInstance(160, 580, Image.SCALE_SMOOTH)));
        } catch (Exception ignored) {}

    
        if (pageCache.containsKey(nom)) {
            cardLayout.show(mainContent, nom);
        }

        pageActuelle = nom;

    
        SwingUtilities.invokeLater(() ->
            scrollPane.getVerticalScrollBar().setValue(0));
    }
   
    private void refreshPageCompte() {
        JPanel nouvellePage = createPageCompte();
        pageCache.put("Compte", nouvellePage);
        mainContent.add(nouvellePage, "Compte");
    }

    private void afficherPageLecture(LivreDetail livre, String pageRetour) {
        String cle = "Lecture_" + livre.titre;
        JPanel page = creerPageLecture(livre, pageRetour);
        pageCache.put(cle, page);
        mainContent.add(page, cle);
        cardLayout.show(mainContent, cle);
        pageActuelle = cle;
        SwingUtilities.invokeLater(() -> scrollPane.getVerticalScrollBar().setValue(0));
    }

    /** Affiche les résultats de recherche multi-livres. */
    private void afficherResultatsRecherche(String query, List<LivreDetail> resultats) {
        String cle = "Recherche_" + query;
        JPanel page = creerPageResultats(query, resultats);
        pageCache.put(cle, page);
        mainContent.add(page, cle);
        cardLayout.show(mainContent, cle);
        pageActuelle = cle;
        SwingUtilities.invokeLater(() -> scrollPane.getVerticalScrollBar().setValue(0));
    }

    // ======================= NOTIFICATION IN-APP =======================
    /** Remplace les JOptionPane de feedback par une notification stylisée. */
    private void afficherNotification(String message) {
        JDialog notif = new JDialog(this, false);
        notif.setUndecorated(true);
        JLabel lbl = new JLabel("  " + message + "  ");
        lbl.setFont(FONT_BODY);
        lbl.setForeground(texteClair);
        lbl.setOpaque(true);
        lbl.setBackground(kola);
        lbl.setBorder(BorderFactory.createLineBorder(acacia, 2));
        lbl.setHorizontalAlignment(SwingConstants.CENTER);
        notif.add(lbl);
        notif.pack();
        // Positionner en bas au centre de la fenêtre principale
        Point loc = getLocationOnScreen();
        notif.setLocation(
            loc.x + getWidth()/2  - notif.getWidth()/2,
            loc.y + getHeight() - 100
        );
        notif.setVisible(true);
        // Disparaît automatiquement après 2 secondes
        javax.swing.Timer timer = new javax.swing.Timer(2000, e -> notif.dispose());
        timer.setRepeats(false);
        timer.start();
    }

    // ======================= HELPER : CHERCHER UN LIVRE =======================
    /**
     * Recherche insensible à la casse dans le catalogue.
     * Centralise toutes les recherches — plus jamais de catalogue.get() direct.
     */
    private Optional<LivreDetail> findLivre(String titre) {
        if (titre == null || titre.isBlank()) return Optional.empty();
        String t = titre.trim();
        // Essai exact d'abord (plus rapide)
        if (catalogue.containsKey(t)) return Optional.of(catalogue.get(t));
        // Sinon recherche insensible à la casse
        return catalogue.values().stream()
            .filter(l -> l.titre.equalsIgnoreCase(t))
            .findFirst();
    }

    // ======================= PERSISTANCE =======================
    private void chargerDonnees() {
        lecturesEnCours = new ArrayList<>();
        aLire           = new ArrayList<>();
        livresTermines  = new ArrayList<>();
        objectifs       = new ArrayList<>();

        // Lectures en cours
        int nbLec = PREFS.getInt("lec_count", -1);
        if (nbLec >= 0) {
            for (int i = 0; i < nbLec; i++) {
                String titre = PREFS.get("lec_titre_" + i, "");
                String prog  = PREFS.get("lec_prog_"  + i, "0");
                if (!titre.isEmpty()) lecturesEnCours.add(new String[]{titre, prog});
            }
        } else {
            lecturesEnCours.add(new String[]{"Petit Pays - Gaël Faye", "45"});
            lecturesEnCours.add(new String[]{"Americanah - Chimamanda Ngozi Adichie", "30"});
        }

        // À lire
        int nbAL = PREFS.getInt("al_count", -1);
        if (nbAL >= 0) {
            for (int i = 0; i < nbAL; i++) {
                String t = PREFS.get("al_" + i, "");
                if (!t.isEmpty()) aLire.add(t);
            }
        } else {
            aLire.add("Femmes, Race et Classe - Angela Davis");
            aLire.add("The Black Atlantic - Paul Gilroy");
            aLire.add("Sulwe - Lupita Nyong'o");
        }

        // Livres terminés
        int nbTerm = PREFS.getInt("term_count", 0);
        for (int i = 0; i < nbTerm; i++) {
            String t = PREFS.get("term_" + i, "");
            if (!t.isEmpty()) livresTermines.add(t);
        }

        // Objectifs
        int nbObj = PREFS.getInt("obj_count", -1);
        if (nbObj >= 0) {
            for (int i = 0; i < nbObj; i++) {
                String t = PREFS.get("obj_" + i, "");
                if (!t.isEmpty()) objectifs.add(t);
            }
        } else {
            objectifs.add("Lire 5 livres par mois");
            objectifs.add("100 livres cette année");
            objectifs.add("Lire plus de classiques");
            objectifs.add("Découvrir de nouveaux auteurs");
        }

        // Profil
        userEmail        = PREFS.get("email",        "user@ozavino.com");
        userInspirations = PREFS.get("inspirations", "Littérature, Art, Histoire");
        nomUtilisateur   = PREFS.get("nom",          "");
        estConnecte      = PREFS.getBoolean("connecte", false);
    }

    private void sauvegarderDonnees() {
        PREFS.putInt("lec_count", lecturesEnCours.size());
        for (int i = 0; i < lecturesEnCours.size(); i++) {
            PREFS.put("lec_titre_" + i, lecturesEnCours.get(i)[0]);
            PREFS.put("lec_prog_"  + i, lecturesEnCours.get(i)[1]);
        }
        PREFS.putInt("al_count", aLire.size());
        for (int i = 0; i < aLire.size(); i++) PREFS.put("al_" + i, aLire.get(i));

        PREFS.putInt("term_count", livresTermines.size());
        for (int i = 0; i < livresTermines.size(); i++) PREFS.put("term_" + i, livresTermines.get(i));

        PREFS.putInt("obj_count", objectifs.size());
        for (int i = 0; i < objectifs.size(); i++) PREFS.put("obj_" + i, objectifs.get(i));

        PREFS.put("email",        userEmail);
        PREFS.put("inspirations", userInspirations);
        PREFS.put("nom",          nomUtilisateur);
        PREFS.putBoolean("connecte", estConnecte);
    }

    // ======================= CATALOGUE COMPLET =======================
   private void remplirCatalogue() {
    // ==========================================================================================
    // LITTÉRATURE & FICTION
    // 

    catalogue.put("Petit Pays", new LivreDetail("Petit Pays", "Gaël Faye", "2016", "Littérature & Fiction",
        "En 1992, Gabriel, dix ans, vit au Burundi avec son père français et sa mère rwandaise dans un quartier privilégié de Bujumbura. Il mène une existence paisible, rythmée par les jeux avec ses copains. Mais ce paradis s'effondre lorsque la guerre civile éclate au Burundi, suivie du génocide des Tutsi au Rwanda. Gabriel voit son innocence voler en éclats et découvre la haine raciale, la violence et l'exil.<br><br>" +
        "<b>Analyse :</b> Une œuvre poignante qui utilise le regard d'un enfant pour traiter de la perte de l'innocence et de la brutalité de l'histoire. Faye réussit à humaniser une tragédie géopolitique complexe à travers le prisme de l'identité métisse et du déracinement.",
        "Disponible", "images/Petit Pays - Edition Collector.jpg"));

    catalogue.put("Americanah", new LivreDetail("Americanah", "Chimamanda Ngozi Adichie", "2013", "Littérature & Fiction",
        "Ifemelu et Obinze sont deux adolescents amoureux dans un Nigeria sous dictature militaire. Ifemelu part étudier aux États-Unis, où elle est confrontée pour la première fois à la réalité d'être 'Noire', un concept qui n'existait pas pour elle à Lagos. Elle devient une blogueuse célèbre analysant les questions de race. Obinze, n'ayant pu la rejoindre, tente sa chance à Londres. Quinze ans plus tard, ils se retrouvent dans un Nigeria transformé.<br><br>" +
        "<b>Analyse :</b> Un roman magistral sur l'immigration et la 'négritude' perçue par les Africains en Occident. Adichie déconstruit les préjugés avec un humour acerbe et une intelligence rare, tout en offrant une magnifique histoire d'amour universelle.",
        "Emprunté", "images/Americanah  Chimamanda Ngozi Adichie Nigeria.jpg"));

    catalogue.put("Segu", new LivreDetail("Segu", "Maryse Condé", "1984", "Littérature & Fiction",
        "Au XVIIIe siècle, le royaume bambara de Ségou est à son apogée, mais les pressions extérieures menacent ses fondations. À travers le destin des quatre fils de la noble famille Traoré, Maryse Condé dépeint le choc des cultures : l'avancée de l'Islam, l'horreur du commerce des esclaves et l'ombre grandissante de la colonisation. Ce roman épique traverse les continents, de l'Afrique au Brésil en passant par la Jamaïque.<br><br>" +
        "<b>Analyse :</b> Condé redonne ici ses lettres de noblesse à l'épopée africaine. Elle explore avec brio comment les traditions ancestrales ont résisté ou muté face aux forces exogènes, soulignant la complexité et la richesse des sociétés précoloniales.",
        "Disponible", "images/Segu Maryse Condé (Guadeloupe).jpg"));

    catalogue.put("Gouverneurs de la rosée", new LivreDetail("Gouverneurs de la rosée", "Jacques Roumain", "1944", "Littérature & Fiction",
        "Après quinze ans passés à couper la canne à Cuba, Manuel revient dans son village natal de Fonds-Rouge en Haïti. Il y découvre une terre aride, dévastée par la sécheresse et une haine ancestrale qui divise les familles. Porteur d'un idéal de solidarité, il part à la recherche d'une source d'eau pour sauver la communauté. Son sacrifice final permettra d'unir les paysans et de faire renaître la vie.<br><br>" +
        "<b>Analyse :</b> Classique de la littérature haïtienne, ce roman est un hymne à la solidarité paysanne et au lien indéfectible entre l'homme et sa terre. L'analyse souligne ici l'esthétique marxiste de Roumain : la rédemption passe par l'action collective plutôt que par la providence divine.",
        "Disponible", "images/Gouverneurs de la rosée – Jacques Roumain (Haïti).jpg"));

    catalogue.put("Ponciá Vicêncio", new LivreDetail("Ponciá Vicêncio", "Conceição Evaristo", "2003", "Littérature & Fiction",
        "Ponciá est une jeune femme noire vivant au Brésil, descendante d'esclaves. Elle quitte son village pour la ville, espérant échapper à la pauvreté et à l'héritage de servitude de sa famille. Le récit suit son voyage intérieur et physique alors qu'elle lutte pour forger son identité dans une société qui cherche à l'effacer. Hantée par les souvenirs de ses ancêtres, elle cherche un moyen de briser le cycle du silence.<br><br>" +
        "<b>Analyse :</b> Evaristo utilise le concept de 'escrevivência' (écrire en vivant) pour explorer la condition de la femme noire brésilienne. L'œuvre analyse finement les traumatismes transgénérationnels de l'esclavage qui persistent dans le Brésil contemporain.",
        "Disponible", "images/Ponciá Vicêncio – Conceição Evaristo (Brésil).jpg"));

    catalogue.put("Changó, el gran putas", new LivreDetail("Changó, el gran putas", "Manuel Zapata Olivella", "1983", "Littérature & Fiction",
        "Épopée mythologique et historique où la diaspora africaine traverse les siècles, guidée par le dieu Yoruba Changó. De la traite transatlantique à la résistance aux Amériques, le roman mêle les voix de figures comme Toussaint Louverture et Malcolm X à des éléments magiques. C'est une célébration de la force spirituelle et de la résilience d'un peuple déraciné qui a su transformer sa souffrance en culture rebelle.<br><br>" +
        "<b>Analyse :</b> Un pilier de la littérature afro-colombienne. Olivella propose une décolonisation de l'histoire en plaçant les divinités africaines au centre du récit de la résistance noire dans le Nouveau Monde.",
        "Disponible", "images/Changó, el gran putas – Manuel Zapata Olivella (Colombie).jpg"));

    
    // HISTOIRE & CIVILISATION

    catalogue.put("Nations nègres et culture", new LivreDetail("Nations nègres et culture", "Cheikh Anta Diop", "1954", "Histoire & Civilisation",
        "Ce texte a révolutionné le monde intellectuel en démontrant scientifiquement que la civilisation de l'Égypte ancienne était une civilisation noire. Diop utilise l'anthropologie, la linguistique et l'histoire pour prouver que l'Afrique noire est la source de la culture égyptienne et l'un des berceaux de la civilisation mondiale. Il appelle les Africains à se réapproprier leur passé pour construire leur futur.<br><br>" +
        "<b>Analyse :</b> Œuvre fondatrice du mouvement afrocentriste. L'analyse met en lumière comment Diop combat le racisme académique européen en utilisant les outils de la science rigoureuse pour restaurer la dignité historique du continent noir.",
        "Disponible", "images/Nations nègres et culture – Cheikh Anta Diop (Sénégal).jpg"));

    catalogue.put("Discours sur le colonialisme", new LivreDetail("Discours sur le colonialisme", "Aimé Césaire", "1950", "Histoire & Civilisation",
        "Pamphlet politique d'une puissance absolue où Césaire affirme que le colonialisme n'a jamais été une entreprise de civilisation, mais une machine de déshumanisation. Il établit un lien direct entre les méthodes coloniales et le nazisme, dénonçant l'hypocrisie d'une Europe qui s'indigne de l'horreur sur son sol alors qu'elle l'a pratiquée ailleurs pendant des siècles.<br><br>" +
        "<b>Analyse :</b> Une critique radicale de la modernité occidentale. Césaire analyse le choc en retour de la colonisation qui finit par 'ensauvager' le colonisateur lui-même, un texte qui reste d'une actualité brûlante dans les débats postcoloniaux.",
        "En Réserve", "images/Discours sur le colonialisme – Aimé Césaire (Martinique).jpg"));

    catalogue.put("The 1619 Project", new LivreDetail("The 1619 Project", "Nikole Hannah-Jones", "2021", "Histoire & Civilisation",
        "Initié par le New York Times, ce projet réévalue l'histoire des États-Unis en plaçant l'arrivée des premiers esclaves africains en 1619 comme le véritable acte fondateur de la nation. Il analyse comment l'esclavage et le racisme systémique ont façonné tous les aspects de la société américaine moderne : économie, santé, musique et démocratie.<br><br>" +
        "<b>Analyse :</b> Un travail journalistique de fond qui redéfinit le narratif national américain. L'analyse souligne l'importance de reconnaître l'apport des Afro-Américains non pas comme une note de bas de page, mais comme le moteur même de la démocratie américaine.",
        "Disponible", "images/The 1619 Project – Nikole Hannah-Jones (USA).jpg"));

    catalogue.put("Peau noire, masques blancs", new LivreDetail("Peau noire, masques blancs", "Frantz Fanon", "1952", "Histoire & Civilisation",
        "Fanon analyse les conséquences psychologiques aliénantes de la colonisation. Il explique comment le racisme impose au Noir de porter un 'masque' pour s'adapter aux critères du monde blanc, créant un complexe d'infériorité profond. C'est un cri pour la désaliénation et la libération de l'être humain des chaînes mentales imposées par les préjugés raciaux.<br><br>" +
        "<b>Analyse :</b> Chef-d'œuvre de psychiatrie sociale. Fanon explore la pathologie de l'oppression et démontre que la libération politique est impossible sans une décolonisation de l'esprit et de la psyché.",
        "Disponible", "images/Peau noire, masques blancs – Frantz Fanon (Martinique).jpg"));

    // SOCIÉTÉ & FÉMINISME

    catalogue.put("Femmes, Race et Classe", new LivreDetail("Femmes, Race et Classe", "Angela Davis", "1981", "Société & Féminisme",
        "Davis propose une analyse pionnière de l'intersectionnalité. Elle démontre comment les luttes pour le droit de vote des femmes ont souvent exclu les femmes noires, et comment le racisme et le capitalisme se conjuguent pour opprimer les travailleuses. Elle retrace l'histoire des mouvements abolitionnistes et féministes pour appeler à une lutte unifiée.<br><br>" +
        "<b>Analyse :</b> Un texte de référence pour le féminisme noir. Davis analyse avec précision pourquoi une approche féministe qui ignore les questions de classe et de race est condamnée à ne servir que les intérêts des plus privilégiés.",
        "Disponible", "images/Femmes, Race et Classe – Angela Davis (USA).jpg"));

    catalogue.put("The Cancer Journals", new LivreDetail("The Cancer Journals", "Audre Lorde", "1980", "Société & Féminisme",
        "Après une mastectomie, Lorde livre un récit intime et politique de sa lutte contre le cancer du sein. Elle refuse le silence et la victimisation, transformant sa douleur en outil de pouvoir. Elle questionne les normes de beauté imposées et souligne l'importance de la solidarité entre femmes noires et lesbiennes pour survivre face à l'oppression sociale.<br><br>" +
        "<b>Analyse :</b> Une méditation sur la vulnérabilité et la force. Lorde analyse le cancer non seulement comme une maladie biologique, mais comme une expérience politique où la reprise de parole est l'acte de résistance ultime.",
        "Disponible", "images/The Cancer Journals – Audre Lorde (USA  Antilles).jpg"));

    catalogue.put("Quarto de Despejo", new LivreDetail("Quarto de Despejo", "Carolina Maria de Jesus", "1960", "Société & Féminisme",
        "Véritable journal intime d'une femme noire vivant dans une favela de São Paulo. Carolina ramasse des vieux papiers pour nourrir ses enfants. Malgré la faim, la violence et le mépris, elle écrit chaque jour sur la réalité brutale de la pauvreté. Son témoignage est devenu un classique mondial de la littérature sociale.<br><br>" +
        "<b>Analyse :</b> Un cri de dignité brute. L'analyse de cette œuvre montre comment l'écriture devient un espace de survie et de visibilité pour les populations marginalisées au sein d'un système urbain impitoyable.",
        "Disponible", "images/Quarto de Despejo – Carolina Maria de Jesus (Brésil).jpg"));

    // ARTS & CULTURE

    catalogue.put("Afropean", new LivreDetail("Afropean", "Johny Pitts", "2019", "Arts & Culture",
        "L'auteur parcourt les métropoles européennes (Paris, Berlin, Lisbonne...) pour documenter l'expérience noire en Europe. Il rejette l'idée d'une identité figée et explore les cultures hybrides qui naissent à l'intersection de l'Afrique et de l'Europe. C'est un carnet de voyage sociologique mettant en lumière des communautés souvent invisibilisées.<br><br>" +
        "<b>Analyse :</b> Un essai qui redéfinit l'Europe moderne. Pitts analyse avec brio le concept de double appartenance, prouvant que l'on peut être pleinement Européen tout en revendiquant son héritage africain.",
        "Disponible", "images/Afropean – Johny Pitts (Europe  Afrique).jpg"));

    catalogue.put("The Black Atlantic", new LivreDetail("The Black Atlantic", "Paul Gilroy", "1993", "Arts & Culture",
        "Gilroy conteste les nationalismes culturels en proposant le concept d'Atlantique noir comme espace d'échange fluide. Il analyse comment la musique (jazz, reggae), la littérature et la politique ont circulé entre l'Afrique, l'Amérique et l'Europe pour former une modernité noire unique qui transcende les frontières géographiques.<br><br>" +
        "<b>Analyse :</b> Une étude révolutionnaire sur la mondialisation. Gilroy démontre que la culture noire est intrinsèquement transnationale et qu'elle a été un acteur majeur dans la construction de la modernité occidentale.",
        "Disponible", "images/The Black Atlantic – Paul Gilroy (Diaspora).jpg"));

    catalogue.put("The Afro-Latin@ Reader", new LivreDetail("The Afro-Latin@ Reader", "Miriam Jiménez Román", "2010", "Arts & Culture",
        "Cette anthologie rassemble des essais, poèmes et documents historiques explorant l'identité afro-latine aux USA. Elle documente les contributions souvent ignorées des Afro-Latinos à la culture et à la politique américaines, tout en analysant les défis spécifiques liés à l'appartenance à deux mondes perçus comme séparés.<br><br>" +
        "<b>Analyse :</b> Un outil essentiel pour comprendre l'invisibilité historique. L'ouvrage analyse comment ces identités multiples remettent en cause les catégories raciales binaires traditionnelles des Amériques.",
        "Disponible", "images/The Afro-Latin@ Reader – Miriam Jiménez Román (Afro-Latino).jpg"));

    // ==========================================================================================
    // ENTREPRENEURIAT & LEADERSHIP
    // ==========================================================================================

    catalogue.put("Year of Yes", new LivreDetail("Year of Yes", "Shonda Rhimes", "2015", "Entrepreneuriat & Leadership",
        "La créatrice de séries à succès (Grey's Anatomy) raconte comment elle a décidé de dire 'oui' pendant un an à tout ce qui l'effrayait. Ce récit personnel et plein d'humour montre comment sortir de sa zone de confort peut transformer une carrière et une vie personnelle, offrant des leçons précieuses sur le leadership et l'affirmation de soi.<br><br>" +
        "<b>Analyse :</b> Au-delà du développement personnel, c'est une analyse sur le pouvoir de la vulnérabilité dans le leadership féminin. Rhimes démontre que la maîtrise de son propre destin commence par l'acceptation de ses peurs.",
        "Disponible", "images/Year of Yes – Shonda Rhimes (USA).jpg"));

    catalogue.put("Leaving the Tarmac", new LivreDetail("Leaving the Tarmac", "Aigboje Aig-Imoukhuede", "2021", "Entrepreneuriat & Leadership",
        "Le récit de l'ascension fulgurante de l'auteur pour transformer Access Bank en l'une des institutions financières les plus puissantes d'Afrique. C'est un manuel de stratégie et de persévérance qui détaille les défis du secteur bancaire au Nigeria et les principes nécessaires pour bâtir une entreprise d'envergure internationale sur le continent.<br><br>" +
        "<b>Analyse :</b> Un cas d'étude réel sur l'excellence africaine. L'analyse met en avant le concept de leadership éthique et de vision à long terme comme moteurs de la transformation économique du continent.",
        "Disponible", "images/Leaving the Tarmac – Aigboje Aig-Imoukhuede (Nigeria).jpg"));

    catalogue.put("The Memo", new LivreDetail("The Memo", "Minda Harts", "2019", "Entrepreneuriat & Leadership",
        "Guide stratégique pour les femmes de couleur naviguant dans un monde de l'entreprise dominé par des codes qui les excluent. Harts aborde les micro-agressions, les écarts de salaire et l'importance du réseautage, proposant des solutions concrètes pour réussir sans compromettre son identité tout en appelant à une véritable inclusion.<br><br>" +
        "<b>Analyse :</b> Une critique constructive du 'Corporate America'. L'analyse souligne que le leadership ne doit pas être un moule unique, et que la diversité est un atout stratégique sous-utilisé par les entreprises.",
        "Disponible", "images/The Memo – Minda Harts (Leadership des femmes de couleur).jpg"));

    // ==========================================================================================
    // JEUNESSE, CONTES & LÉGENDES
    // ==========================================================================================

    catalogue.put("Akissi", new LivreDetail("Akissi", "Marguerite Abouet", "2010", "Jeunesse, Contes & Légendes",
        "Bande dessinée inspirée de l'enfance de l'auteur en Côte d'Ivoire. On suit les péripéties d'Akissi, une petite fille espiègle qui transforme chaque corvée en aventure. Qu'il s'agisse de poursuivre un singe ou d'éviter les bêtises de ses frères, Akissi nous plonge dans un Abidjan vibrant, joyeux et authentique.<br><br>" +
        "<b>Analyse :</b> Abouet déconstruit les clichés misérabilistes sur l'Afrique. L'analyse montre comment l'humour et le quotidien d'une enfant célèbrent une enfance universelle, tout en valorisant la culture urbaine africaine.",
        "Disponible", "images/Akissi – Marguerite Abouet (Côte d’Ivoire).jpg"));

    catalogue.put("Sulwe", new LivreDetail("Sulwe", "Lupita Nyong'o", "2019", "Jeunesse, Contes & Légendes",
        "Sulwe est née avec la peau couleur de minuit. Elle se sent moins belle que sa sœur au teint clair. Un voyage magique à travers le ciel nocturne lui apprend l'histoire des sœurs Nuit et Jour, lui faisant comprendre que la beauté ne dépend pas de la clarté. Un conte magnifique sur l'importance de s'aimer tels que l'on est.<br><br>" +
        "<b>Analyse :</b> Un ouvrage crucial sur le colorisme. Nyong'o analyse avec poésie l'impact des standards de beauté racialisés sur les enfants, offrant un outil puissant d'estime de soi pour les jeunes filles noires.",
        "Disponible", "images/Sulwe – Lupita Nyong'o (Kenya  Mexique).jpg"));

    catalogue.put("Children of Blood and Bone", new LivreDetail("Children of Blood and Bone", "Tomi Adeyemi", "2018", "Jeunesse, Contes & Légendes",
        "Dans le royaume d'Orïsha, la magie a disparu sur ordre d'un roi impitoyable. Zélie entame une quête épique inspirée par la mythologie Yoruba pour ramener les pouvoirs de son peuple. Accompagnée d'un prince rebelle, elle affronte de nombreux dangers. Ce roman de fantasy aborde des thèmes de justice sociale et d'héritage culturel.<br><br>" +
        "<b>Analyse :</b> Un renouveau de la fantasy mondiale. L'analyse met en relief l'importance de l'Afrofantasy : utiliser le folklore africain pour créer des mondes imaginaires aussi riches que les mythologies européennes classiques.",
        "Disponible", "images/Children of Blood and Bone – Tomi Adeyemi (Afrofantasy - USA  Nigeria).jpg"));
}

    // ======================= PAGE DE LECTURE =======================
    /**
     * Reçoit désormais pageRetour pour que le bouton ← ramène
     * à la bonne page (Catégories, Accueil, Compte, etc.).
     */
    private JPanel creerPageLecture(LivreDetail livre, String pageRetour) {
        JPanel p = new JPanel(null);
        p.setBackground(fond);
        p.setPreferredSize(new Dimension(900, 850));

        // Breadcrumb : Catégories > Genre > Titre
        JLabel breadcrumb = new JLabel(t("categories") + " › " + livre.genre + " › " + livre.titre);
        breadcrumb.setFont(FONT_SMALL);
        breadcrumb.setForeground(acacia);
        breadcrumb.setBounds(40, 10, 820, 22);
        breadcrumb.setCursor(new Cursor(Cursor.HAND_CURSOR));
        breadcrumb.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) { showPage("Catégories"); }
        });
        p.add(breadcrumb);

        JPanel mainPanel = new JPanel(null);
        mainPanel.setBounds(40, 40, 820, 760);
        mainPanel.setBackground(fond);
        p.add(mainPanel);

        // Couverture avec bords arrondis
        JPanel coverCard = createRoundedPanel(kola);
        coverCard.setLayout(new BorderLayout());
        coverCard.setBounds(0, 0, 280, 400);
        coverCard.setOpaque(false);
        coverCard.setBorder(BorderFactory.createLineBorder(acacia, 2));
        JLabel coverLabel = new JLabel();
        coverLabel.setHorizontalAlignment(SwingConstants.CENTER);
        coverLabel.setIcon(loadImage(livre.cheminCouverture, 260, 380));
        coverLabel.setText(coverLabel.getIcon() == null ? "📚 " + livre.titre : null);
        if (coverLabel.getIcon() == null) {
            coverLabel.setFont(FONT_BODY);
            coverLabel.setForeground(texteClair);
        }
        coverCard.add(coverLabel, BorderLayout.CENTER);
        mainPanel.add(coverCard);

        int rectX = 310, rectWidth = 490;

        // Bloc 1 : Titre
        JPanel titreRect = createRoundedPanel(kola);
        titreRect.setBounds(rectX, 0, rectWidth, 60);
        titreRect.setLayout(new BorderLayout());
        JLabel titreLabel = new JLabel(livre.titre, SwingConstants.CENTER);
        titreLabel.setFont(FONT_TITRE);
        titreLabel.setForeground(acacia);
        titreRect.add(titreLabel, BorderLayout.CENTER);
        mainPanel.add(titreRect);

        // Bloc 2 : Auteur, date, statut
        JPanel infoRect = createRoundedPanel(kola);
        infoRect.setBounds(rectX, 75, rectWidth, 60);
        infoRect.setLayout(new GridLayout(1, 3, 10, 0));
        infoRect.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        JLabel auteurLabel = new JLabel("✍️ " + livre.auteur);
        auteurLabel.setFont(FONT_SMALL); auteurLabel.setForeground(texteClair);
        JLabel dateLabel = new JLabel("📅 " + livre.date);
        dateLabel.setFont(FONT_SMALL); dateLabel.setForeground(texteClair);
        String statutIcon = livre.statut.equals("Disponible") ? "✅" : "📖";
        JLabel statutLabel = new JLabel(statutIcon + " " + livre.statut);
        statutLabel.setFont(FONT_SMALL);
        statutLabel.setForeground(livre.statut.equals("Disponible") ? VERT_OK : acacia);
        infoRect.add(auteurLabel); infoRect.add(dateLabel); infoRect.add(statutLabel);
        mainPanel.add(infoRect);

        // Bloc 3 : Synopsis
        JPanel synopsisRect = createRoundedPanel(kola);
        synopsisRect.setBounds(rectX, 150, rectWidth, 350);
        synopsisRect.setLayout(new BorderLayout());
        synopsisRect.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        JLabel synopsisTitle = new JLabel("📖 SYNOPSIS");
        synopsisTitle.setFont(new Font("Serif", Font.BOLD, 16));
        synopsisTitle.setForeground(acacia);
        synopsisTitle.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        JTextArea synopsisArea = new JTextArea(livre.synopsis);
        synopsisArea.setLineWrap(true); synopsisArea.setWrapStyleWord(true);
        synopsisArea.setOpaque(false);  synopsisArea.setEditable(false);
        synopsisArea.setFont(FONT_SMALL); synopsisArea.setForeground(texteClair);
        JScrollPane synopsisScroll = new JScrollPane(synopsisArea);
        synopsisScroll.setBorder(null); synopsisScroll.setOpaque(false);
        synopsisScroll.getViewport().setOpaque(false);
        synopsisRect.add(synopsisTitle, BorderLayout.NORTH);
        synopsisRect.add(synopsisScroll, BorderLayout.CENTER);
        mainPanel.add(synopsisRect);

        // Boutons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        buttonPanel.setBounds(rectX, 520, rectWidth, 60);
        buttonPanel.setOpaque(false);

        // ← Retour : pointe vers pageRetour (pas forcément "Catégories")
        JButton backBtn = creerBouton("⟵ " + t("retour"), argile);
        backBtn.addActionListener(e -> showPage(pageRetour));

        // Sauvegarder → À lire
        JButton saveBtn = creerBouton("📚 " + t("sauvegarder"), ciel);
        saveBtn.addActionListener(e -> {
            if (!estConnecte) {
                afficherNotification(t("connecter_pour_sauvegarder"));
                return;
            }
            String entry = livre.titre + " - " + livre.auteur;
            if (!aLire.contains(entry)) {
                aLire.add(entry);
                sauvegarderDonnees();
                afficherNotification("✅ " + livre.titre + " " + t("ajoute_liste"));
            } else {
                afficherNotification("ℹ️ " + livre.titre + " " + t("deja_liste"));
            }
        });

        // Commencer → Lecture en cours
        JButton startBtn = creerBouton("📖 " + t("commencer"), new Color(60, 120, 60));
        startBtn.addActionListener(e -> {
            if (!estConnecte) {
                afficherNotification(t("connecter_pour_sauvegarder"));
                return;
            }
            String entryLec = livre.titre + " - " + livre.auteur;
            boolean dejaEnCours = lecturesEnCours.stream()
                .anyMatch(l -> l[0].equalsIgnoreCase(entryLec));
            if (dejaEnCours) {
                afficherNotification("ℹ️ " + livre.titre + " " + t("deja_en_cours"));
            } else {
                lecturesEnCours.add(new String[]{entryLec, "0"});
                // Retirer de À lire si présent
                aLire.remove(entryLec);
                sauvegarderDonnees();
                afficherNotification("📖 " + livre.titre + " " + t("commence"));
            }
        });

        // Acheter → Amazon
        JButton buyBtn = creerBouton("💰 " + t("acheter"), argile);
        buyBtn.addActionListener(e -> {
            try {
                Desktop.getDesktop().browse(new URI(
                    "https://www.amazon.fr/s?k=" + livre.titre.replace(" ", "+")));
            } catch (Exception ex) {
                afficherNotification(t("erreur_ouverture"));
            }
        });

        buttonPanel.add(backBtn);
        buttonPanel.add(saveBtn);
        buttonPanel.add(startBtn);
        buttonPanel.add(buyBtn);
        mainPanel.add(buttonPanel);

        return p;
    }

    // ======================= PAGE RÉSULTATS DE RECHERCHE =======================
    private JPanel creerPageResultats(String query, List<LivreDetail> resultats) {
        JPanel p = new JPanel(null);
        p.setBackground(fond);
        p.setPreferredSize(new Dimension(900, 750));

        JLabel title = new JLabel(t("resultats_recherche") + " : \"" + query + "\" (" + resultats.size() + ")", SwingConstants.CENTER);
        title.setFont(FONT_TITRE);
        title.setForeground(kola);
        title.setBounds(0, 20, 900, 40);
        p.add(title);

        JPanel grid = new JPanel(new FlowLayout(FlowLayout.CENTER, 40, 30));
        grid.setBackground(fond);

        for (LivreDetail livre : resultats) {
            grid.add(creerCartelivre(livre, "Accueil"));
        }

        JScrollPane scroll = new JScrollPane(grid);
        scroll.setBounds(50, 80, 800, 550);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        p.add(scroll);

        JButton back = creerBouton("⟵ " + t("retour"), kola);
        back.setBounds(390, 650, 120, 35);
        back.addActionListener(e -> showPage("Accueil"));
        p.add(back);

        return p;
    }

    // ======================= PAGE CATÉGORIES =======================
    private JPanel createPageCategories() {
        JPanel p = new JPanel(null);
        p.setBackground(fond);
        p.setPreferredSize(new Dimension(900, 850));

        String[] categories = {
            "Littérature & Fiction", "Histoire & Civilisation",
            "Société & Féminisme", "Arts & Culture",
            "Entrepreneuriat & Leadership", "Jeunesse, Contes & Légendes"
        };
        String[] imgFiles = {
            "cat_literature&fiction.png.jpg", "cat_histoire&civilisation.png.jpg",
            "cat_société&feminisme.png.jpg",   "cat_art,musique&culture.PNG.jpg",
            "cat_entreprenariat&leadership.png.jpg", "cat_jeunesse,conte&legende.png.jpg"
        };
        int[] xPos = {50, 480, 50, 480, 50, 480};
        int[] yPos = {30, 30, 320, 320, 610, 610};

        for (int i = 0; i < categories.length; i++) {
            final String catName = categories[i];
            JPanel card = createRoundedPanel(kola);
            card.setLayout(null);
            card.setBounds(xPos[i], yPos[i], 380, 260);
            card.setBorder(BorderFactory.createLineBorder(acacia, 2));
            card.setOpaque(false);
            card.setCursor(new Cursor(Cursor.HAND_CURSOR)); // CORRECTION : curseur main

            JLabel imgLabel = new JLabel();
            imgLabel.setBounds(115, 20, 150, 150);
            imgLabel.setIcon(loadImage("images/" + imgFiles[i], 150, 150));
            if (imgLabel.getIcon() == null) imgLabel.setText("🖼️");
            card.add(imgLabel);

            // Nombre de livres dans la catégorie
            long nbLivres = catalogue.values().stream().filter(l -> l.genre.equals(catName)).count();
            JLabel nbLabel = new JLabel(nbLivres + " " + t("livres"), SwingConstants.CENTER);
            nbLabel.setForeground(texteClair);
            nbLabel.setFont(FONT_SMALL);
            nbLabel.setBounds(0, 170, 380, 20);
            card.add(nbLabel);

            JLabel lbl = new JLabel(catName, SwingConstants.CENTER);
            lbl.setForeground(acacia);
            lbl.setFont(new Font("Serif", Font.BOLD, 16));
            lbl.setBounds(0, 190, 380, 25);
            card.add(lbl);

            JButton btn = creerBouton(t("explorer"), argile);
            btn.setBounds(130, 218, 120, 30);
            btn.addActionListener(e -> afficherListeLivres(catName));
            card.add(btn);
            p.add(card);
        }
        return p;
    }

    // ======================= PAGE LISTE DES LIVRES =======================
    private void afficherListeLivres(String categorie) {
        String cle = "Liste_" + categorie;
        JPanel page = creerPageListeLivres(categorie, "Tous");
        pageCache.put(cle, page);
        mainContent.add(page, cle);
        cardLayout.show(mainContent, cle);
        pageActuelle = cle;
        SwingUtilities.invokeLater(() -> scrollPane.getVerticalScrollBar().setValue(0));
    }

    private JPanel creerPageListeLivres(String categorie, String filtreStatut) {
        JPanel p = new JPanel(null);
        p.setBackground(fond);
        p.setPreferredSize(new Dimension(900, 800));

        JLabel title = new JLabel(categorie, SwingConstants.CENTER);
        title.setFont(new Font("Serif", Font.BOLD, 26));
        title.setForeground(kola);
        title.setBounds(0, 15, 900, 35);
        p.add(title);

        // Filtre par statut (NOUVEAU)
        JLabel filtreLabel = new JLabel(t("filtrer") + " :");
        filtreLabel.setForeground(kola);
        filtreLabel.setFont(FONT_SMALL);
        filtreLabel.setBounds(50, 60, 70, 25);
        p.add(filtreLabel);

        String[] statuts = {t("tous"), "Disponible", "Emprunté", "En Réserve"};
        JComboBox<String> filtreBox = new JComboBox<>(statuts);
        filtreBox.setBackground(texteClair);
        filtreBox.setForeground(kola);
        filtreBox.setBounds(125, 60, 140, 25);
        filtreBox.setSelectedItem(filtreStatut.equals("Tous") ? t("tous") : filtreStatut);
        p.add(filtreBox);

        // Grille de livres dans un JScrollPane
        JScrollPane[] scrollRef = new JScrollPane[1];

        Runnable rafraichirGrille = () -> {
            String statutChoisi = (String) filtreBox.getSelectedItem();
            JPanel grid = new JPanel(new FlowLayout(FlowLayout.CENTER, 40, 30));
            grid.setBackground(fond);

            for (LivreDetail livre : catalogue.values()) {
                if (!livre.genre.equals(categorie)) continue;
                if (!statutChoisi.equals(t("tous")) && !livre.statut.equals(statutChoisi)) continue;
                grid.add(creerCartelivre(livre, "Liste_" + categorie));
            }

            if (scrollRef[0] != null) p.remove(scrollRef[0]);
            JScrollPane scroll = new JScrollPane(grid);
            scroll.setBounds(50, 100, 800, 570);
            scroll.setBorder(null);
            scroll.getVerticalScrollBar().setUnitIncrement(16);
            p.add(scroll);
            scrollRef[0] = scroll;
            p.revalidate();
            p.repaint();
        };

        filtreBox.addActionListener(e -> rafraichirGrille.run());
        rafraichirGrille.run();

        JButton back = creerBouton("⟵ " + t("retour"), kola);
        back.setBounds(390, 690, 120, 35);
        back.addActionListener(e -> showPage("Catégories"));
        p.add(back);

        return p;
    }

    /** Crée une carte livre réutilisable (Catégories, Recherche, etc.) */
    private JPanel creerCartelivre(LivreDetail livre, String pageRetour) {
        JPanel bookCard = createRoundedPanel(kola);
        bookCard.setLayout(null);
        bookCard.setPreferredSize(new Dimension(200, 320));
        bookCard.setOpaque(false);
        bookCard.setBorder(BorderFactory.createLineBorder(acacia, 1));
        bookCard.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JLabel cover = new JLabel();
        cover.setBounds(10, 10, 180, 220);
        cover.setHorizontalAlignment(SwingConstants.CENTER);
        ImageIcon icon = loadImage(livre.cheminCouverture, 180, 220);
        if (icon != null) cover.setIcon(icon);
        else { cover.setText("📖"); cover.setFont(new Font("Serif", Font.PLAIN, 40)); }

        // Badge de statut
        String badgeText = livre.statut.equals("Disponible") ? "✅" :
                           livre.statut.equals("Emprunté")   ? "📖" : "📦";
        JLabel badge = new JLabel(badgeText);
        badge.setBounds(155, 10, 30, 20);

        JLabel name = new JLabel(
            "<html><center>" + livre.titre + "<br><i>" + livre.auteur + "</i></center></html>",
            SwingConstants.CENTER);
        name.setBounds(0, 240, 200, 70);
        name.setForeground(texteClair);
        name.setFont(FONT_SMALL);

        bookCard.add(cover);
        bookCard.add(badge);
        bookCard.add(name);
        bookCard.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                afficherPageLecture(livre, pageRetour);
            }
        });
        return bookCard;
    }

    // ======================= PAGE ACCUEIL =======================
    private JPanel createPageAccueil() {
        JPanel p = new JPanel(null);
        p.setBackground(fond);
        p.setPreferredSize(new Dimension(900, 700));

        JPanel rect = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(kola);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 30, 30));
                g2.dispose();
            }
        };
        rect.setOpaque(false);
        rect.setLayout(new BoxLayout(rect, BoxLayout.Y_AXIS));
        rect.setBounds(30, 20, 280, 600);
        rect.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 20));

        // Liens de la colonne gauche associés à des livres pertinents
        addSection(rect, t("nouveautes"),
            new String[]{t("livres_recents"), t("actualite"), t("disponibilites")},
            new String[]{"The 1619 Project", "Afropean", "Sulwe"});
        addSection(rect, t("recommandations"),
            new String[]{t("auteur_interessant"), t("artiste_prometteur"), t("selection_staff")},
            new String[]{"Americanah", "Children of Blood and Bone", "Petit Pays"});
        addSection(rect, t("evenements"),
            new String[]{t("groupes_lecture"), t("dedicaces"), t("masterclass")},
            new String[]{"Femmes, Race et Classe", "Nations nègres et culture", "Year of Yes"});
        p.add(rect);

        JPanel centerPanel = new JPanel(null);
        centerPanel.setBackground(kola);
        centerPanel.setBounds(340, 20, 520, 500);
        centerPanel.setBorder(BorderFactory.createLineBorder(acacia, 1));

        JLabel popularTitle = new JLabel(t("populaires"));
        popularTitle.setForeground(acacia);
        popularTitle.setFont(new Font("Serif", Font.BOLD, 20));
        popularTitle.setBounds(10, 10, 300, 30);
        centerPanel.add(popularTitle);

        String[] popularBooks = {
            "Petit Pays - Gaël Faye",
            "Americanah - Chimamanda Ngozi Adichie",
            "Sulwe - Lupita Nyong'o"
        };
        for (int i = 0; i < popularBooks.length; i++) {
            JLabel book = new JLabel("📚 " + popularBooks[i]);
            book.setForeground(texteClair);
            book.setFont(FONT_BODY);
            book.setBounds(20, 50 + i * 40, 480, 30);
            book.setCursor(new Cursor(Cursor.HAND_CURSOR));
            final String titre = popularBooks[i].split(" - ")[0];
            book.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    findLivre(titre).ifPresent(l -> afficherPageLecture(l, "Accueil"));
                }
            });
            centerPanel.add(book);
        }

        JLabel quote = new JLabel("<html><center>\"Les bibliothèques sont pleines d'idées —<br>peut-être les armes les plus dangereuses<br>et les plus puissantes qui soient\"</center></html>");
        quote.setForeground(acacia);
        quote.setFont(new Font("Serif", Font.ITALIC, 14));
        quote.setBounds(20, 220, 480, 100);
        centerPanel.add(quote);
        p.add(centerPanel);

        // Bouton Spotify
        try {
            JLabel spotifyLabel = new JLabel();
            spotifyLabel.setIcon(loadImage("images/music_page1.jpg", 180, 80));
            spotifyLabel.setBounds(380, 550, 180, 80);
            spotifyLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
            spotifyLabel.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    try { Desktop.getDesktop().browse(new URI("https://open.spotify.com/playlist/1MkCR8odOrFM4QOZ2QKF7w")); }
                    catch (Exception ex) { afficherNotification(t("erreur_ouverture")); }
                }
            });
            p.add(spotifyLabel);
        } catch (Exception ignored) {}

        return p;
    }

    /**
     * addSection améliorée : chaque lien pointe vers un livre pertinent
     * (plus de redirection arbitraire vers "Petit Pays").
     */
    private void addSection(JPanel parent, String titre, String[] items, String[] livresAssocies) {
        JLabel tLabel = new JLabel(titre);
        tLabel.setForeground(acacia);
        tLabel.setFont(FONT_SECTION);
        parent.add(tLabel);
        parent.add(Box.createVerticalStrut(5));
        for (int i = 0; i < items.length; i++) {
            JLabel link = new JLabel("• " + items[i]);
            link.setForeground(texteClair);
            link.setFont(FONT_SMALL);
            link.setCursor(new Cursor(Cursor.HAND_CURSOR));
            final String livreAssocie = (livresAssocies != null && i < livresAssocies.length)
                ? livresAssocies[i] : "Petit Pays";
            link.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    findLivre(livreAssocie).ifPresent(l -> afficherPageLecture(l, "Accueil"));
                }
            });
            parent.add(link);
            parent.add(Box.createVerticalStrut(8));
        }
        parent.add(Box.createVerticalStrut(25));
    }

    // ======================= PAGE CONNEXION =======================
    private JPanel createPageConnexion() {
        JPanel p = new JPanel(null);
        p.setBackground(fond);
        p.setPreferredSize(new Dimension(900, 600));

        JPanel loginPanel = new JPanel(null);
        loginPanel.setBackground(kola);
        loginPanel.setBounds(250, 150, 400, 300);
        loginPanel.setBorder(BorderFactory.createLineBorder(acacia, 2));

        JLabel titleLabel = new JLabel(t("connexion_titre"), SwingConstants.CENTER);
        titleLabel.setForeground(acacia);
        titleLabel.setFont(new Font("Serif", Font.BOLD, 24));
        titleLabel.setBounds(0, 30, 400, 40);
        loginPanel.add(titleLabel);

        JLabel userLabel = new JLabel(t("nom_utilisateur") + " :");
        userLabel.setForeground(texteClair);
        userLabel.setBounds(50, 100, 120, 25);
        loginPanel.add(userLabel);

        JTextField userField = new JTextField();
        userField.setBounds(180, 100, 170, 25);
        userField.setBackground(texteClair);
        loginPanel.add(userField);

        JLabel passLabel = new JLabel(t("mot_de_passe") + " :");
        passLabel.setForeground(texteClair);
        passLabel.setBounds(50, 150, 120, 25);
        loginPanel.add(passLabel);

        JPasswordField passField = new JPasswordField();
        passField.setBounds(180, 150, 170, 25);
        passField.setBackground(texteClair);
        loginPanel.add(passField);

        JButton loginBtn = creerBouton(t("se_connecter"), argile);
        loginBtn.setBounds(150, 210, 100, 35);

        ActionListener loginAction = e -> {
            String user = userField.getText().trim();
            String pass = new String(passField.getPassword());
            if (user.isEmpty()) { afficherNotification(t("entrer_nom")); return; }
            if (pass.isEmpty()) { afficherNotification(t("entrer_mdp"));  return; }
            estConnecte    = true;
            nomUtilisateur = user;
            sauvegarderDonnees();
            afficherNotification(t("bienvenue") + " " + user + " !");
            showPage("Compte");
        };
        loginBtn.addActionListener(loginAction);
        // Touche Entrée dans le champ mot de passe
        passField.addActionListener(loginAction);

        loginPanel.add(loginBtn);
        p.add(loginPanel);
        return p;
    }

    // ======================= PAGE COMPTE =======================
    private JPanel createPageCompte() {
        JPanel p = new JPanel(null);
        p.setBackground(fond);
        p.setPreferredSize(new Dimension(900, 950));

        if (!estConnecte) {
            JPanel notConnectedPanel = new JPanel(null);
            notConnectedPanel.setBackground(kola);
            notConnectedPanel.setBounds(200, 200, 500, 200);
            notConnectedPanel.setBorder(BorderFactory.createLineBorder(acacia, 2));
            JLabel message = new JLabel(t("non_connecte"), SwingConstants.CENTER);
            message.setForeground(acacia);
            message.setFont(new Font("Serif", Font.BOLD, 18));
            message.setBounds(0, 50, 500, 40);
            notConnectedPanel.add(message);
            JButton loginBtn = creerBouton(t("se_connecter"), argile);
            loginBtn.setBounds(200, 120, 100, 35);
            loginBtn.addActionListener(e -> showPage("Connexion"));
            notConnectedPanel.add(loginBtn);
            p.add(notConnectedPanel);
            return p;
        }

        // Panneau utilisateur
        JPanel userPanel = new JPanel(null);
        userPanel.setBackground(kola);
        userPanel.setBounds(50, 20, 800, 180);
        userPanel.setBorder(BorderFactory.createLineBorder(acacia, 1));

        JLabel welcomeLabel = new JLabel(t("bienvenue") + ", " + nomUtilisateur + " !");
        welcomeLabel.setForeground(acacia);
        welcomeLabel.setFont(new Font("Serif", Font.BOLD, 20));
        welcomeLabel.setBounds(30, 20, 400, 30);
        userPanel.add(welcomeLabel);

        JButton editProfileBtn = creerBouton("✎", argile);
        editProfileBtn.setFont(new Font("Serif", Font.BOLD, 14));
        editProfileBtn.setBounds(440, 20, 40, 30);
        editProfileBtn.addActionListener(e -> editerProfil());
        userPanel.add(editProfileBtn);

        String[][] champs = {
            {t("nom"),         nomUtilisateur},
            {t("email"),       userEmail},
            {t("inspirations"),userInspirations}
        };
        for (int i = 0; i < champs.length; i++) {
            JLabel lbl = new JLabel("• " + champs[i][0] + " :");
            lbl.setForeground(acacia); lbl.setBounds(30, 60 + i*35, 110, 25);
            JLabel val = new JLabel(champs[i][1]);
            val.setForeground(texteClair); val.setBounds(150, 60 + i*35, 400, 25);
            userPanel.add(lbl); userPanel.add(val);
        }
        p.add(userPanel);

        // Lecture en cours (titres cliquables)
        JPanel readingPanel = new JPanel(null);
        readingPanel.setBackground(kola);
        readingPanel.setBounds(50, 220, 380, 300);
        readingPanel.setBorder(BorderFactory.createLineBorder(acacia, 1));

        JLabel readingTitle = new JLabel(t("lecture_cours"));
        readingTitle.setForeground(acacia);
        readingTitle.setFont(new Font("Serif", Font.BOLD, 16));
        readingTitle.setBounds(15, 15, 350, 25);
        readingPanel.add(readingTitle);

        int yOffset = 50;
        for (int i = 0; i < lecturesEnCours.size(); i++) {
            String[] livre = lecturesEnCours.get(i);
            JLabel book = new JLabel("📚 " + livre[0]);
            book.setForeground(texteClair);
            book.setBounds(15, yOffset, 250, 25);
            // Titre cliquable
            book.setCursor(new Cursor(Cursor.HAND_CURSOR));
            final String titreLec = livre[0].split(" - ")[0];
            book.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    findLivre(titreLec).ifPresent(l -> afficherPageLecture(l, "Compte"));
                }
            });
            readingPanel.add(book);

            JProgressBar progress = new JProgressBar(0, 100);
            progress.setValue(Integer.parseInt(livre[1]));
            progress.setBounds(15, yOffset + 30, 270, 15);
            progress.setBackground(fond); progress.setForeground(argile);
            readingPanel.add(progress);

            JButton editBtn = creerBouton("✎", argile);
            editBtn.setBounds(290, yOffset, 40, 25);
            final int idx = i;
            editBtn.addActionListener(e -> editerLectureEnCours(idx));
            readingPanel.add(editBtn);
            yOffset += 75;
        }
        p.add(readingPanel);

        // À lire (titres cliquables)
        JPanel toReadPanel = new JPanel(null);
        toReadPanel.setBackground(kola);
        toReadPanel.setBounds(470, 220, 380, 300);
        toReadPanel.setBorder(BorderFactory.createLineBorder(acacia, 1));

        JLabel toReadTitle = new JLabel(t("a_lire_titre"));
        toReadTitle.setForeground(acacia);
        toReadTitle.setFont(new Font("Serif", Font.BOLD, 16));
        toReadTitle.setBounds(15, 15, 350, 25);
        toReadPanel.add(toReadTitle);

        yOffset = 50;
        for (int i = 0; i < aLire.size(); i++) {
            String entry = aLire.get(i);
            JLabel book = new JLabel("⭐ " + entry);
            book.setForeground(texteClair);
            book.setBounds(15, yOffset, 220, 25);
            book.setCursor(new Cursor(Cursor.HAND_CURSOR));
            final String titreLivre = entry.split(" - ")[0];
            book.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    findLivre(titreLivre).ifPresent(l -> afficherPageLecture(l, "Compte"));
                }
            });
            toReadPanel.add(book);

            JButton editBtn = creerBouton("✎", argile);
            editBtn.setBounds(240, yOffset, 35, 25);
            final int idx = i;
            editBtn.addActionListener(e -> editerALire(idx));
            toReadPanel.add(editBtn);

            JButton delBtn = creerBouton("🗑", rosehip);
            delBtn.setBounds(278, yOffset, 35, 25);
            delBtn.addActionListener(e -> supprimerALire(idx));
            toReadPanel.add(delBtn);
            yOffset += 40;
        }

        JButton addBookBtn = creerBouton("+ " + t("ajouter_livre"), argile);
        addBookBtn.setBounds(15, yOffset + 10, 150, 30);
        addBookBtn.addActionListener(e -> ajouterALire());
        toReadPanel.add(addBookBtn);
        p.add(toReadPanel);

        // Objectifs
        JPanel goalsPanel = new JPanel(null);
        goalsPanel.setBackground(kola);
        goalsPanel.setBounds(50, 540, 800, 130);
        goalsPanel.setBorder(BorderFactory.createLineBorder(acacia, 1));

        JLabel goalsTitle = new JLabel(t("objectifs_lecture"));
        goalsTitle.setForeground(acacia);
        goalsTitle.setFont(new Font("Serif", Font.BOLD, 16));
        goalsTitle.setBounds(15, 15, 300, 25);
        goalsPanel.add(goalsTitle);

        // Compteur livres terminés
        JLabel termLabel = new JLabel("✅ " + livresTermines.size() + " " + t("livres_termines"));
        termLabel.setForeground(VERT_OK);
        termLabel.setFont(FONT_SMALL);
        termLabel.setBounds(550, 15, 230, 25);
        goalsPanel.add(termLabel);

        yOffset = 50;
        for (int i = 0; i < objectifs.size(); i++) {
            JLabel goal = new JLabel("🎯 " + objectifs.get(i));
            goal.setForeground(texteClair);
            goal.setBounds(15 + (i%2)*390, yOffset + (i/2)*35, 320, 25);
            goalsPanel.add(goal);
            JButton editGoalBtn = creerBouton("✎", argile);
            editGoalBtn.setBounds(15 + (i%2)*390 + 295, yOffset + (i/2)*35, 35, 25);
            final int idxGoal = i;
            editGoalBtn.addActionListener(e -> editerObjectif(idxGoal));
            goalsPanel.add(editGoalBtn);
        }
        p.add(goalsPanel);

        // Boutons généraux
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 50, 10));
        buttonPanel.setOpaque(false);
        buttonPanel.setBounds(200, 700, 500, 60);

        JButton modifierBtn = creerBouton(t("modifier_infos"), argile);
        modifierBtn.setPreferredSize(new Dimension(200, 40));
        modifierBtn.addActionListener(e -> editerProfil());

        JButton deconnecterBtn = creerBouton(t("deconnecter"), argile);
        deconnecterBtn.setPreferredSize(new Dimension(200, 40));
        deconnecterBtn.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(p,
                t("confirm_deconnexion"), t("deconnexion"), JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                estConnecte    = false;
                nomUtilisateur = "";
                sauvegarderDonnees();
                afficherNotification(t("deconnexion_reussie"));
                showPage("Accueil"); // ← Correction : plus de createPageCompte() inutile
            }
        });

        buttonPanel.add(modifierBtn);
        buttonPanel.add(deconnecterBtn);
        p.add(buttonPanel);
        return p;
    }

    // ======================= MÉTHODES D'ÉDITION =======================
    /** refreshCompte() : méthode unique pour rafraîchir le Compte après une édition. */
    private void refreshCompte() {
        sauvegarderDonnees();
        refreshPageCompte();
        showPage("Compte");
    }

    private void editerProfil() {
        JPanel panel = new JPanel(new GridLayout(0, 2, 10, 10));
        panel.setBackground(kola);
        JTextField nomField          = new JTextField(nomUtilisateur);
        JTextField emailField        = new JTextField(userEmail);
        JTextField inspirationsField = new JTextField(userInspirations);
        panel.add(new JLabel(t("nom") + " :"));          panel.add(nomField);
        panel.add(new JLabel(t("email") + " :"));        panel.add(emailField);
        panel.add(new JLabel(t("inspirations") + " :")); panel.add(inspirationsField);
        int result = JOptionPane.showConfirmDialog(this, panel, t("modifier_profil"),
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            nomUtilisateur   = nomField.getText().trim();
            userEmail        = emailField.getText().trim();
            userInspirations = inspirationsField.getText().trim();
            refreshCompte();
        }
    }

    private void editerLectureEnCours(int index) {
        String[] livre = lecturesEnCours.get(index);
        JPanel panel = new JPanel(new GridLayout(0, 2, 10, 10));
        panel.setBackground(kola);
        JTextField titreField = new JTextField(livre[0]);
        JSlider progressionSlider = new JSlider(0, 100, Integer.parseInt(livre[1]));
        progressionSlider.setPaintTicks(true); progressionSlider.setPaintLabels(true);
        progressionSlider.setMajorTickSpacing(25); progressionSlider.setMinorTickSpacing(5);
        panel.add(new JLabel("Titre :"));           panel.add(titreField);
        panel.add(new JLabel("Progression (%) :")); panel.add(progressionSlider);
        int result = JOptionPane.showConfirmDialog(this, panel, "Modifier le livre", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            int progression = progressionSlider.getValue();
            lecturesEnCours.set(index, new String[]{titreField.getText(), String.valueOf(progression)});

            // Si terminé à 100%, proposer de marquer comme terminé
            if (progression == 100) {
                String titreLu = titreField.getText().split(" - ")[0];
                int terminer = JOptionPane.showConfirmDialog(this,
                    "Félicitations ! Marquer \"" + titreLu + "\" comme terminé ?",
                    "Livre terminé", JOptionPane.YES_NO_OPTION);
                if (terminer == JOptionPane.YES_OPTION) {
                    livresTermines.add(titreField.getText());
                    lecturesEnCours.remove(index);
                    afficherNotification("🎉 Livre terminé ajouté à votre palmarès !");
                }
            }
            refreshCompte();
        }
    }

    private void editerALire(int index) {
        String nouveauTitre = JOptionPane.showInputDialog(this, "Nouveau titre :", aLire.get(index));
        if (nouveauTitre != null && !nouveauTitre.trim().isEmpty()) {
            aLire.set(index, nouveauTitre.trim());
            refreshCompte();
        }
    }

    private void supprimerALire(int index) {
        int confirm = JOptionPane.showConfirmDialog(this,
            "Supprimer \"" + aLire.get(index) + "\" ?", "Confirmation", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            aLire.remove(index);
            refreshCompte();
        }
    }

    private void ajouterALire() {
        String nouveauTitre = JOptionPane.showInputDialog(this, "Nouveau livre à lire :");
        if (nouveauTitre != null && !nouveauTitre.trim().isEmpty()) {
            aLire.add(nouveauTitre.trim());
            refreshCompte();
        }
    }

    private void editerObjectif(int index) {
        String nouvelObjectif = JOptionPane.showInputDialog(this, "Modifier l'objectif :", objectifs.get(index));
        if (nouvelObjectif != null && !nouvelObjectif.trim().isEmpty()) {
            objectifs.set(index, nouvelObjectif.trim());
            refreshCompte();
        }
    }

    // ======================= TRADUCTION =======================
    private void traduireApplication() {
        String pageAvant = pageActuelle; // Mémoriser la page courante
        if ("FR".equals(langueActuelle)) setTitle("OZAVINO - Bibliothèque Universelle");
        else if ("EN".equals(langueActuelle)) setTitle("OZAVINO - Universal Library");
        else setTitle("OZAVINO - Biblioteca Universal");

        rebuildStaticPages();
        showPage(pageAvant); // Retourner à la page d'avant la traduction
    }

    private void remplirDico() {
        dico.put("accueil",      new String[]{"Accueil",    "Home",       "Inicio"});
        dico.put("compte",       new String[]{"Compte",     "Account",    "Cuenta"});
        dico.put("categories",   new String[]{"Catégories", "Categories", "Categorías"});
        dico.put("nouveautes",   new String[]{"Nouveautés", "What's New", "Novedades"});
        dico.put("recommandations", new String[]{"Recommandations", "Recommended",    "Recomendaciones"});
        dico.put("evenements",   new String[]{"Évènements", "Events",     "Eventos"});
        dico.put("livres_recents", new String[]{"Livres récents",  "Recent Books",   "Libros Recientes"});
        dico.put("actualite",    new String[]{"L'actualité", "News",       "Actualidad"});
        dico.put("disponibilites", new String[]{"Disponibilités", "Availability", "Disponibilidades"});
        dico.put("auteur_interessant", new String[]{"Auteur intéressant", "Interesting Author",  "Autor Interesante"});
        dico.put("artiste_prometteur", new String[]{"Artiste prometteur", "Promising Artist",    "Artista Prometedor"});
        dico.put("selection_staff",    new String[]{"Sélection Staff",    "Staff Selection",     "Selección Staff"});
        dico.put("groupes_lecture",    new String[]{"Groupes de lecture", "Reading Groups",      "Grupos de Lectura"});
        dico.put("dedicaces",    new String[]{"Dédicaces",  "Signings",   "Dedicatorias"});
        dico.put("masterclass",  new String[]{"Masterclass","Masterclass","Masterclass"});
        dico.put("populaires",   new String[]{"Populaires", "Popular",    "Populares"});
        dico.put("connexion_titre", new String[]{"CONNEXION",       "LOGIN",          "INICIAR SESIÓN"});
        dico.put("nom_utilisateur", new String[]{"Nom d'utilisateur","Username",       "Nombre de usuario"});
        dico.put("mot_de_passe",    new String[]{"Mot de passe",     "Password",       "Contraseña"});
        dico.put("se_connecter",    new String[]{"SE CONNECTER",     "LOG IN",         "INICIAR SESIÓN"});
        dico.put("entrer_nom",   new String[]{"Veuillez entrer un nom",          "Please enter a name",      "Ingrese un nombre"});
        dico.put("entrer_mdp",   new String[]{"Veuillez entrer un mot de passe", "Please enter a password",  "Ingrese una contraseña"});
        dico.put("bienvenue",    new String[]{"Bienvenue",  "Welcome",    "Bienvenido"});
        dico.put("non_connecte", new String[]{"Vous n'êtes pas connecté", "Not logged in", "No has iniciado sesión"});
        dico.put("nom",          new String[]{"NOM",        "NAME",       "NOMBRE"});
        dico.put("email",        new String[]{"EMAIL",      "EMAIL",      "CORREO"});
        dico.put("inspirations", new String[]{"INSPIRATIONS","INSPIRATIONS","INSPIRACIONES"});
        dico.put("lecture_cours",new String[]{"Lecture en cours", "Currently Reading", "Leyendo ahora"});
        dico.put("a_lire_titre", new String[]{"À lire",     "To Read",    "Por leer"});
        dico.put("objectifs_lecture", new String[]{"Objectifs de lecture", "Reading Goals", "Objetivos de lectura"});
        dico.put("modifier_infos",    new String[]{"MODIFIER MES INFORMATIONS", "EDIT MY INFO",    "EDITAR MIS DATOS"});
        dico.put("modifier_profil",   new String[]{"Modifier le profil",         "Edit profile",    "Editar perfil"});
        dico.put("ajouter_livre",     new String[]{"Ajouter un livre", "Add a book", "Añadir libro"});
        dico.put("deconnecter",  new String[]{"SE DÉCONNECTER", "LOG OUT",        "CERRAR SESIÓN"});
        dico.put("confirm_deconnexion", new String[]{"Voulez-vous vous déconnecter ?", "Do you want to log out?", "¿Quieres cerrar sesión?"});
        dico.put("deconnexion",  new String[]{"Déconnexion",    "Logout",         "Cerrar sesión"});
        dico.put("deconnexion_reussie", new String[]{"Déconnexion réussie !", "Logged out successfully!", "¡Sesión cerrada!"});
        dico.put("explorer",     new String[]{"EXPLORER",   "EXPLORE",    "EXPLORAR"});
        dico.put("retour",       new String[]{"RETOUR",     "BACK",       "VOLVER"});
        dico.put("langues",      new String[]{"Langues",    "Languages",  "Idiomas"});
        dico.put("aucun_resultat", new String[]{"Aucun résultat pour", "No results for",     "Sin resultados para"});
        dico.put("erreur_ouverture", new String[]{"Impossible d'ouvrir le lien", "Cannot open link", "No se puede abrir"});
        dico.put("sauvegarder",  new String[]{"Sauvegarder","Save",       "Guardar"});
        dico.put("acheter",      new String[]{"Acheter",    "Buy",        "Comprar"});
        dico.put("commencer",    new String[]{"Commencer",  "Start",      "Empezar"});
        dico.put("connecter_pour_sauvegarder", new String[]{
            "Veuillez vous connecter pour sauvegarder des livres",
            "Please log in to save books",
            "Por favor inicia sesión para guardar libros"});
        dico.put("ajoute_liste",   new String[]{"ajouté à votre liste !",  "added to your list!",    "añadido a tu lista!"});
        dico.put("deja_liste",     new String[]{"est déjà dans votre liste.", "already in your list.", "ya está en tu lista."});
        dico.put("deja_en_cours",  new String[]{"est déjà en cours.",        "already in progress.",   "ya está en progreso."});
        dico.put("commence",       new String[]{"ajouté aux lectures en cours !", "added to current reads!", "añadido a lecturas actuales!"});
        dico.put("resultats_recherche", new String[]{"Résultats", "Results", "Resultados"});
        dico.put("filtrer",        new String[]{"Filtrer",   "Filter",     "Filtrar"});
        dico.put("tous",           new String[]{"Tous",      "All",        "Todos"});
        dico.put("livres",         new String[]{"livres",    "books",      "libros"});
        dico.put("livres_termines",new String[]{"livres terminés", "books completed", "libros terminados"});
    }

    // ======================= UTILITAIRES =======================

    /** Charge une image avec fallback null si introuvable. */
    private ImageIcon loadImage(String path, int w, int h) {
        try {
            ImageIcon raw = new ImageIcon(path);
            if (raw.getIconWidth() <= 0) return null;
            return new ImageIcon(raw.getImage().getScaledInstance(w, h, Image.SCALE_SMOOTH));
        } catch (Exception e) {
            return null;
        }
    }

    /** Crée un bouton stylisé avec la charte graphique. */
    private JButton creerBouton(String texte, Color bg) {
        JButton b = new JButton(texte);
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setFont(FONT_BTN);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return b;
    }

    /** Crée un panel avec fond arrondi — réutilisé sur toutes les pages. */
    private JPanel createRoundedPanel(Color bg) {
        return new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g2.dispose();
            }
        };
    }

    /** Style la scrollbar avec la charte graphique. */
    private void styleScrollBar(JScrollPane sp) {
        sp.getVerticalScrollBar().setUI(new BasicScrollBarUI() {
            @Override protected void configureScrollBarColors() {
                this.thumbColor = new Color(212, 163, 115, 150);
                this.trackColor = new Color(0, 0, 0, 0);
            }
            @Override protected JButton createDecreaseButton(int o) { return emptyBtn(); }
            @Override protected JButton createIncreaseButton(int o) { return emptyBtn(); }
            private JButton emptyBtn() {
                JButton b = new JButton(); b.setPreferredSize(new Dimension(0,0)); return b;
            }
            @Override protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(212,163,115,180));
                g2.fillRoundRect(r.x, r.y, r.width, r.height, 10, 10);
                g2.dispose();
            }
        });
    }

    /** Crée un bouton de menu avec icône. */
    private JButton createMenuBtn(String txt, String imgPath, String page) {
        ImageIcon icon = loadImage(imgPath, 50, 50);
        JButton b = (icon != null) ? new JButton(txt, icon) : new JButton(txt);
        b.setVerticalTextPosition(SwingConstants.BOTTOM);
        b.setHorizontalTextPosition(SwingConstants.CENTER);
        b.setFont(FONT_SMALL);
        b.setForeground(texteClair);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        b.addActionListener(e -> showPage(page));
        return b;
    }

    /** Traduction rapide. */
    private String t(String cle) {
        int i = "FR".equals(langueActuelle) ? 0 : ("EN".equals(langueActuelle) ? 1 : 2);
        return dico.getOrDefault(cle, new String[]{cle, cle, cle})[i];
    }

    // ======================= MAIN =======================
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new OzavinoApp().setVisible(true));
    }
}