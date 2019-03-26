
# Exemple de documentation utilisateur convertie en HTML à partir d'un Markdown

Une page de documentation peut être rédigée en Markdown pour être convertie en HTML, comme celle-ci ; mais elle peut
également être directement rédigée en HTML comme [celle-là](sample_page2.html).

Il faut veiller, lors de l'inclusion de liens internes à la documentation, à ne pas référencer les noms de fichiers au 
format Markdown mais les noms des fichiers HTML générés à partir de la documentation en Markdown. Il suffit pour cela
de remplacer l'extension .md par l'extension .html.

Les patrons de la documentation contiennent les liens vers les styles de mise en forme et les scripts de coloration syntaxique.

## Un titre principal de la page de documentation

Un exemple de code avec coloration syntaxique :

Code :
```Java
import com.examind.storage.DataSet;
import com.examind.storage.FeatureDataSet;
import com.examind.storage.FeatureResource;
import org.apache.sis.feature.DefaultFeatureType;

try (final DataSet ds = ...) {
  if (ds instanceof FeatureDataSet) {
      final FeatureDataSet fds = (FeatureDataSet) ds;
      /* On commence par chercher les identifiants des lots de données contenus
       * dans le dataset.
       */
      final Set<String> types;
      try {
          types = fds.getResourceNames();
      } catch (DataStoreException ex) {
          /* Si on ne parvient pas à se connecter à la source de données, ou
           * qu'une erreur survient lors de l'analyse de son contenu.
           */
          throw ex;
      }

      /* Une fois la liste construite, on demande la structure associée à chaque
       * nom, et on l'imprime
       */
      for (final String name : types) {
          try {
              final FeatureResource fr = fds.fetch(name);
              final DefaultFeatureType type = fr.getType();
              System.out.println(type);
          } catch (DataStoreException e) {
              // If we cannot connect to datasource subset.
              System.out.println("Cannot connect to data type "+name);
          }
      }
  }
}
```

Une sortie mise en forme :

```shell
FR166230_S57
┌────────────────┬──────────┬─────────────┬───────────────────┐
│ Nom            │ Type     │ Cardinalité │ Valeur par défaut │
├────────────────┼──────────┼─────────────┼───────────────────┤
│ sis:envelope   │ Envelope │ [1 … 1]     │ = Envelope()      │
│ sis:geometry   │ Geometry │ [1 … 1]     │ = geometry        │
│ sis:identifier │ String   │ [1 … 1]     │                   │
│ RCNM           │ Integer  │ [1 … 1]     │                   │
│ RCID           │ Long     │ [1 … 1]     │                   │
│ PRIM           │ Integer  │ [1 … 1]     │                   │
│ GRUP           │ Integer  │ [1 … 1]     │                   │
│ OBJL           │ Integer  │ [1 … 1]     │                   │
│ RVER           │ Integer  │ [1 … 1]     │                   │
│ RUIN           │ Integer  │ [1 … 1]     │                   │
│ AGEN           │ Integer  │ [1 … 1]     │                   │
│ FIDN           │ Integer  │ [1 … 1]     │                   │
│ FIDS           │ Integer  │ [1 … 1]     │                   │
│ geometry       │ Geometry │ [1 … 1]     │                   │
│ vectors        │ vector   │ [0 … ∞]     │                   │
└────────────────┴──────────┴─────────────┴───────────────────┘

```

Un log mis en forme :

```logs
Nombre d'entités : 82
```

Du XML mis en forme :

Filtre XML :

```XML
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<ogc:Filter xmlns:ogc="http://www.opengis.net/ogc">
    <ogc:PropertyIsGreaterThan>
        <ogc:PropertyName>FIDS</ogc:PropertyName>
            <ogc:Literal>1</ogc:Literal>
    </ogc:PropertyIsGreaterThan>
</ogc:Filter>
```