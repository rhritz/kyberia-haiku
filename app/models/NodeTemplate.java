package models;

// definuje sposob zobrazenie aktualneho node,

import java.util.List;

// v zavislosti od user, viewtemplate
// node moze byt zobrazeny roznymi NodeTemplate

import com.google.code.morphia.annotations.Entity;


@Entity
public class NodeTemplate extends MongoEntity {

    public static final int BASIC_NODE = 1;

	String Id;
	String htmlTemplate;
	String script;
	String css;
	List<TemplateDataDef> wantedData;
	// otazka je ako s datami a ich spracovanim
	// potrebujeme nejaku factory, kde z Appl zavolame Template.giveMeTemplate(...)
	// alebo kao ?
	// napriklad listNodes je v pdostate templatova funkcia

	// tj najvacsi zmysel asi dava ak bude kazda template jedna trieda / *alebo html/groovy skript!*
	// kontrakt bude:
	// - ak ide o Location, tak uz sme v specializovanej funkcii v Application a nie je co riesit, data proste
        // mame k dispozicii
	// - ak ide o Node, tak asi by Template mala mat hash s nazvami pozadovnaych dat a
	// v Application bude nieco ako:



	public static void render()
	{
	}

        public List<TemplateDataDef> getWantedData()
        {
            return null;
        }
}