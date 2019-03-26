package org.constellation.dto.thesaurus;


import java.util.Comparator;

/**
 * Compare two thesaurus DTO objects.
 *
 * @author Quentin Boileau (Geomatys)
 */
public class ThesaurusComparator implements Comparator<Thesaurus> {

    private final String field;
    private final boolean asc;

    public ThesaurusComparator(String field, boolean asc) {
        this.field = field;
        this.asc = asc;
    }

    @Override
    public int compare(Thesaurus t1, Thesaurus t2) {
        int result;

        switch (field) {
            case "name":
                result = t1.getName().compareTo(t2.getName());
                break;
            case "creationDate":
                result = t1.getCreationDate().compareTo(t2.getCreationDate());
                break;
            default:
                throw new IllegalArgumentException("Thesaurus can not be sorted on field \"" + field + "\".");
        }

        return asc ? -result : result;
    }

}
