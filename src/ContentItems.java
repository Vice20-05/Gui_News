
import java.util.Objects;

/** Represents one displayable item and its field names and values. */
public class ContentItems {
    private final String type;
    // fieldNames[i] is the label for values[i], for example "Title" -> "Some headline".
    private final String[] fieldNames;
    private String[] values;

    /** Creates an item after validating and copying its data. */
    public ContentItems(String type, String[] fieldNames, String[] values) {
        this.type = Objects.requireNonNull(type, "type cannot be null");
        this.fieldNames = copyAndNormalize(fieldNames, "fieldNames");
        this.values = copyAndNormalize(values, "values");
        if (this.fieldNames.length == 0 || this.fieldNames.length != this.values.length) {
            throw new IllegalArgumentException(
                    "fieldNames and values must have the same non-zero length"
            );
        }
    }

    /** Returns the item's category, such as News or Event. */
    public String getType() {
        return type;
    }

    /** Returns a defensive copy of the field names. */
    public String[] getFieldNames() {
        return fieldNames.clone();
    }

    /** Returns a defensive copy of the field values. */
    public String[] getValues() {
        return values.clone();
    }

    /** Replaces the values while preserving the field count. */
    public void setValues(String[] values) {
        String[] newValues = copyAndNormalize(values, "values");
        if (newValues.length != fieldNames.length) {
            throw new IllegalArgumentException("values must match the number of field names");
        }
        this.values = newValues;
    }

    /** Returns the value of the given field, or an empty string if there is no such field. */
    public String getValue(String fieldName) {
        for (int i = 0; i < fieldNames.length; i++) {
            if (fieldNames[i].equals(fieldName)) {
                return values[i];
            }
        }
        return "";
    }

    /** Returns the first value, which is treated as the title. */
    public String getTitle() {
        return values[0];
    }

    /** Returns the publish or event date stored in the item. */
    public String getSummary() {
        for (int i = 0; i < fieldNames.length; i++) {
            if (fieldNames[i].equals("Publish date") || fieldNames[i].equals("Event date")) {
                return values[i];
            }
        }
        return "";
    }

    /** The text shown for this item in the list on the left. */
    @Override
    public String toString() {
        return type + "  |  " + getTitle();
    }

    /** Copies the array so outside code can't change it, and turns null values into "". */
    private String[] copyAndNormalize(String[] source, String name) {
        Objects.requireNonNull(source, name + " cannot be null");
        String[] copy = source.clone();
        for (int i = 0; i < copy.length; i++) {
            if (copy[i] == null) {
                copy[i] = "";
            }
        }
        return copy;
    }
}
