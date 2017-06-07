
import java.io.PrintWriter;
import java.util.*;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Hit;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.LockObtainFailedException;

@WebServlet("/ServletTesina")
public class ServletFinale extends HttpServlet {

    public static final String FILES_TO_INDEX_DIRECTORY = "C:/Users/simon/Documents/NetBeansProjects/Servlet/filesToIndex";       // cartella dei file da indicizzare
    public static final String INDEX_DIRECTORY = "C:/Users/simon/Documents/NetBeansProjects/Servlet/indexDirectory";              // cartella dell'index

    public static final String FIELD_PATH = "path";
    public static final String FIELD_CONTENTS = "contents";

    public static void createIndex() throws CorruptIndexException, LockObtainFailedException, IOException { //Creo l'index
        Analyzer analyzer = new StandardAnalyzer();
        boolean ricreaIndex = true;
        /* Un oggetto IndexWriter è usato per creare e aggiornare l'index. Qui ho utilizzato un costruttore che prende 3 argomenti.
         * Il primo argomento è il precorso della cartella locale dove vengono salvati i file index.
         * Il secondo argomento è un oggetto StandardAnalyzer. Un analyzer rappresenta l'insieme di regole che vengono utilizzate per estrarre i termini index dal testo.
         * Il terzo argomento è un parametro booleano che, se impostato su true, dice all'IndexWriter di ricreare nuovamente l'index se ne è già presente uno.*/
        IndexWriter indexWriter = new IndexWriter(INDEX_DIRECTORY, analyzer, ricreaIndex);
        File dir = new File(FILES_TO_INDEX_DIRECTORY);
        File[] files = dir.listFiles();
        /* Per ogni file creo un documento Lucene, che contiene il contenuto, i metadata e altre informazioni, e gli aggiungo due campi.
         *Il primo contiene la path di ogni file nell'index, mentre il secondo rappresenta il contenuto del file.*/
        for (File file : files) {
            Document document = new Document();

            String path = file.getCanonicalPath();
            document.add(new Field(FIELD_PATH, path, Field.Store.YES, //specifico di mantenere i file nell'index
                    Field.Index.UN_TOKENIZED));                                 //specifico di non far spezzettare la path dall' Analyzer

            Reader reader = new FileReader(file);
            document.add(new Field(FIELD_CONTENTS, reader));    //il contenuto viene spezzettato e indicizzato.    
            indexWriter.addDocument(document);
        }
        indexWriter.close();
    }

    public static String searchIndex(String searchString) throws IOException, ParseException {
        String response;
        Directory directory = FSDirectory.getDirectory(INDEX_DIRECTORY);
        IndexReader indexReader = IndexReader.open(directory);              //apre l'index
        IndexSearcher indexSearcher = new IndexSearcher(indexReader);       //cerca nell'index

        Analyzer analyzer = new StandardAnalyzer();
        QueryParser queryParser = new QueryParser(FIELD_CONTENTS, analyzer);
        Query query = queryParser.parse(searchString);                      //analizza i termini di ricerca
        Hits risultati = indexSearcher.search(query);                       //definisce i risultati
        response = "<p>Risultati trovati per '<strong>" + searchString + "</strong>': " + risultati.length() + "</p>";

        Iterator<Hit> it = risultati.iterator();                            //cerca tutti i risultati
        while (it.hasNext()) {
            Hit risultato = it.next();
            Document document = risultato.getDocument();
            String path = document.get(FIELD_PATH);                         //definisce la path del risultato
            response += "<p>Risultato trovato in: " + path + "</p>\n";
        }

        return response;
    }

    @Override
    protected void doPost(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {

        String searchResponseHTML = "";
        String takeTerms = request.getParameter("terms");                   //prende i termini da cercare
        List<String> items = Arrays.asList(takeTerms.split(" "));           //i termini vengono divisi e cercati individualmente
        String[] strarray = items.toArray(new String[0]);
        createIndex();
        for (String strarray1 : strarray) {
            try {
                searchResponseHTML += searchIndex(strarray1);
            } catch (ParseException ex) {
                Logger.getLogger(ServletFinale.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        // creo il costruttore per la risposta
        PrintWriter writer = response.getWriter();

        // Costruisco il codice HTML
        String htmlResponse = "<html>"
                + "<head>"
                + "<title>Lucene Web Search</title>\n"
                + "<script>\n"
                + "function goBack() {\n"
                + "window.history.back();\n"
                + "}\n"
                + "</script>\n"
                + "<link rel=\"stylesheet\" type=\"text/css\" href=\"theme.css\">\n"
                + "</head>";
        htmlResponse += "<h2>Hai cercato: " + items + "<br/></h2>";
        htmlResponse += "<p>" + searchResponseHTML + "</p><br/>"
                + "<button class=\"button2\" onclick=\"goBack()\"><span>Indietro </span></button>";
        htmlResponse += "</html>";

        // mando la risposta
        writer.println(htmlResponse);

    }

}
