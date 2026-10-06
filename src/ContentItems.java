
import java.util.Objects;

public class ContentItems {
    private final String type;
    private final String[] fieldNames;
    private String[] values;

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

    public String getType() {
        return type;
    }

    public String[] getFieldNames() {
        return fieldNames.clone();
    }

    public String[] getValues() {
        return values.clone();
    }

    public void setValues(String[] values) {
        String[] newValues = copyAndNormalize(values, "values");
        if (newValues.length != fieldNames.length) {
            throw new IllegalArgumentException("values must match the number of field names");
        }
        this.values = newValues;
    }

    public String getTitle() {
        return values[0];
    }

    public String getSummary() {
        for (int i = 0; i < fieldNames.length; i++) {
            if (fieldNames[i].equals("Publish date") || fieldNames[i].equals("Event date")) {
                return values[i];
            }
        }
        return "";
    }

    @Override
    public String toString() {
        return type + "  |  " + getTitle();
    }

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
