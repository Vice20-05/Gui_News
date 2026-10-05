
public class ContentItems {
    private final String type;
    private final String[] fieldNames;
    private String[] values;

    public ContentItems(String type, String[] fieldNames, String[] values) {
        this.type = type;
        this.fieldNames = fieldNames.clone();
        this.values = values.clone();
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
        this.values = values.clone();
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

}
