package hgo.ipprint4;

/**
 * Created by hgode on 08.04.2014.
 */
public class PrintFileDetails {
    public String shortname;
    public String description;
    public String help;
    public String filename;
    public PrintLanguage.ePrintLanguages printLanguage;
    public Integer printerWidth=2;

    public PrintFileDetails(){
        this.printerWidth=2;
        this.printLanguage= PrintLanguage.ePrintLanguages.NAN;
        this.description="undefined";
        this.filename="no file";
        this.help="empty entry";
        this.shortname="do not use";
    }
    @Override
    public String toString(){
        String s="unknown";
        s=String.format("description: %s\nprint language: %s\nprint width: %s\nfile name: %s",
                this.description,
                this.printLanguage,
                this.printerWidth,
                this.filename);
        return s;
    }
    public String getName(){
        return this.filename;
    }
}