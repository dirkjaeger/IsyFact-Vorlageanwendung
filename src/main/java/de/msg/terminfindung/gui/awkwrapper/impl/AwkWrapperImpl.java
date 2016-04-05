package de.msg.terminfindung.gui.awkwrapper.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.dozer.Mapper;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/*
 * #%L
 * Terminfindung
 * %%
 * Copyright (C) 2015 - 2016 Bundesverwaltungsamt (BVA), msg systems ag
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */


import de.msg.terminfindung.common.exception.TerminfindungBusinessException;
import de.msg.terminfindung.common.konstanten.FehlerSchluessel;
import de.msg.terminfindung.core.erstellung.Erstellung;
import de.msg.terminfindung.core.teilnahme.Teilnahme;
import de.msg.terminfindung.core.verwaltung.Verwaltung;
import de.msg.terminfindung.gui.awkwrapper.AwkWrapper;
import de.msg.terminfindung.gui.terminfindung.model.PraeferenzModel;
import de.msg.terminfindung.gui.terminfindung.model.TagModel;
import de.msg.terminfindung.gui.terminfindung.model.TeilnehmerModel;
import de.msg.terminfindung.gui.terminfindung.model.TeilnehmerZeitraumModel;
import de.msg.terminfindung.gui.terminfindung.model.TerminfindungModel;
import de.msg.terminfindung.gui.terminfindung.model.ZeitraumModel;
import de.msg.terminfindung.persistence.entity.Praeferenz;
import de.msg.terminfindung.persistence.entity.Tag;
import de.msg.terminfindung.persistence.entity.Teilnehmer;
import de.msg.terminfindung.persistence.entity.Terminfindung;
import de.msg.terminfindung.persistence.entity.Zeitraum;

/**
 * Implementierung des Anwendungskern-Wrappers.
 *
 * @author Dirk Jäger
 */
@Transactional(rollbackFor = Throwable.class, propagation = Propagation.REQUIRED)
public class AwkWrapperImpl implements AwkWrapper {

	private static final Logger LOG = Logger.getLogger(AwkWrapperImpl.class);
	
	/**
	 * Die Komponente des Anwendungskerns für die Erstellung von Terminfindungen 
	 */
	private Erstellung erstellung;
	/**
	 * Die Komponente des Anwendungskerns für die Erstellung von Terminfindungen 
	 */
	private Verwaltung verwaltung;
	/**
	 * Die Komponente des Anwendungskerns für die Teilnahme an Terminfindungen
	 */
	private Teilnahme teilnahme;
	/**
	 * Der Bean-Mapper für die Abbildung zwischen View-Objekten und Persistenz-Objekten
	 */
	private Mapper beanMapper;
	

	/* (non-Javadoc)
	 * @see de.msg.terminfindung.gui.awkwrapper.AwkWrapper#erstelleTerminfindung(java.lang.String, java.lang.String, java.util.List)
	 */
	public TerminfindungModel erstelleTerminfindung(String organisatorName,
			String veranstaltungsName, List<TagModel> tage) throws TerminfindungBusinessException {

		List<Tag> termine = new ArrayList<>();
		List<Zeitraum> zeitraeume;
		for (TagModel tagModel : tage) {

			Tag tag = new Tag(tagModel.getDatum());

			zeitraeume = new ArrayList<>();
			for (ZeitraumModel zeitraumModel : tagModel.getZeitraeume()) {

				Zeitraum zeitraum = new Zeitraum(zeitraumModel.getBeschreibung());
				zeitraeume.add(zeitraum);
				
			}
			tag.setZeitraeume(zeitraeume);
			termine.add(tag);
		}		
		
		Terminfindung terminfindung = erstellung.erstelleTerminfindung(organisatorName, veranstaltungsName, termine);

		//gib die Terminfindung als Ergebnis zurück
		return map(terminfindung);		
	}
		
	/* (non-Javadoc)
	 * @see de.msg.terminfindung.gui.awkwrapper.AwkWrapper#ladeTerminfindung(long)
	 */
	public TerminfindungModel ladeTerminfindung(long terminfindungsNr) throws TerminfindungBusinessException {

		Terminfindung tf = verwaltung.leseTerminfindung(terminfindungsNr);
		return map(tf);	
	}

	/* (non-Javadoc)
	 * @see de.msg.terminfindung.gui.awkwrapper.AwkWrapper#setzeVeranstaltungstermin(de.msg.terminfindung.gui.terminfindung.model.TerminfindungModel, long)
	 */
	public TerminfindungModel setzeVeranstaltungstermin (TerminfindungModel terminfindungModel, long zeitraumNr) throws TerminfindungBusinessException  {
		
		if (terminfindungModel == null) throw new TerminfindungBusinessException(FehlerSchluessel.MSG_PARAMETER_UNGUELTIG);
		
		Terminfindung terminfindung = verwaltung.leseTerminfindung(terminfindungModel.getTerminfnd_Nr());

		verwaltung.setzeVeranstaltungstermin (terminfindung, zeitraumNr);
		
		// gib die aktualisierte Terminfindung als Ergebnis zurück
		return map(terminfindung);
	}
	
	/* (non-Javadoc)
	 * @see de.msg.terminfindung.gui.awkwrapper.AwkWrapper#bestaetigeTeilnahme(de.msg.terminfindung.gui.terminfindung.model.TerminfindungModel, de.msg.terminfindung.gui.terminfindung.model.TeilnehmerModel, java.util.Map)
	 */
	public TerminfindungModel bestaetigeTeilnahme(TerminfindungModel terminfindungModel, TeilnehmerModel teilnehmerModel, Map<ZeitraumModel, PraeferenzModel> viewTerminwahl) throws TerminfindungBusinessException  {
		
		if (terminfindungModel == null || teilnehmerModel == null || viewTerminwahl == null) 
			throw new TerminfindungBusinessException(FehlerSchluessel.MSG_PARAMETER_UNGUELTIG);

		// Übertrage die Datenstrukturen aus dem View in die Struktur des Anwendungskerns
		// Lese die Terminfindung anhand ihrer Id
		Terminfindung terminfindung = verwaltung.leseTerminfindung(terminfindungModel.getTerminfnd_Nr());
	
		// Der Teilnehmer wird neu erzeugt, der Name wird übertragen
		Teilnehmer teilnehmer = new Teilnehmer();
		teilnehmer.setName(teilnehmerModel.getName());
		
		// Suche in den gegebenen Zeiträumen der Terminwahl nach den IDs der Zeiträume, die in der Map übergeben wurden
		// Konstruiere daraus die entsprechende Map für den Aufruf des Anwendungskerns
		Map<Zeitraum, Praeferenz> terminwahl = new HashMap <>();
		for (ZeitraumModel zeitraumModel : viewTerminwahl.keySet()) {

			Zeitraum zeitraum = terminfindung.findeZeitraumById(zeitraumModel.getZeitraum_nr());

			// Wenn in der Terminfindung kein Zeitraum mit der gesuchten Id exisistiert ist die Anfrage ungültig
			if (zeitraum == null) throw new TerminfindungBusinessException(FehlerSchluessel.MSG_PARAMETER_UNGUELTIG);

			// Bilder den View-Präferenzwert auf den entsprechenden Persistenz-Präferenzwert ab und speichere
			Praeferenz praeferenz = map(viewTerminwahl.get(zeitraumModel));
			terminwahl.put(zeitraum, praeferenz);
		}
		// rufe den Anwendungskern auf
		teilnahme.bestaetigeTeilnahme(terminfindung, teilnehmer, terminwahl);
		
		// gib die aktualisierte Terminfindung als Ergebnis zurück
		return map(terminfindung);
	}

	
	/* (non-Javadoc)
	 * @see de.msg.terminfindung.gui.awkwrapper.AwkWrapper#loescheZeitraum(de.msg.terminfindung.gui.terminfindung.model.TerminfindungModel, de.msg.terminfindung.gui.terminfindung.model.ZeitraumModel)
	 */
	public TerminfindungModel loescheZeitraeume(TerminfindungModel terminfindungModel, List<ZeitraumModel> viewZeitraeume) throws TerminfindungBusinessException{

		if (terminfindungModel == null || viewZeitraeume == null )
			throw new TerminfindungBusinessException(FehlerSchluessel.MSG_PARAMETER_UNGUELTIG);

		// Übertrage die Datenstrukturen aus dem View in die Struktur des Anwendungskerns
		// Lese die Terminfindung anhand ihrer Id, Konstruiere die entsprechende Liste für den Aufruf des
		// Anwendungskerns
		List<Zeitraum> zeitraumList = new ArrayList<>();
		Terminfindung terminfindung = verwaltung.leseTerminfindung(terminfindungModel.getTerminfnd_Nr());

		// Hole zu jedem zu löschenden Zeitraum das entsprechende Objekt des Anwendungskerns
		for (ZeitraumModel zeitraumModel : viewZeitraeume) {

			Zeitraum zeitraum = terminfindung.findeZeitraumById(zeitraumModel.getZeitraum_nr());
			// Wenn in der Terminfindung kein Zeitraum mit der gesuchten Id exisistiert ist die Anfrage ungültig
			if (zeitraum == null) throw new TerminfindungBusinessException(FehlerSchluessel.MSG_PARAMETER_UNGUELTIG);

			zeitraumList.add(zeitraum);
		}

		// rufe den Anwendungskern auf
		verwaltung.loescheZeitraeume(terminfindung, zeitraumList);

		// gib die aktualisierte Terminfindung als Ergebnis zurück
		return map(terminfindung);
	}

	/**
	 * Kapselt das Mapping zwischen dem Persistenz-Objekt und dem View-Objekt
	 * einer Terminfindung.
	 * Die Methode verwendet intern Dozer für das eigentlichen Mapping.
	 * Zusätzlich werden weitere Datenstrukturen im Zielobjekt initialisiert.
	 * 
	 * @param terminfindung Das Persistenz-Objekt der Terminfindung
	 * @return Das View-Objekt der Terminfindung
	 */
	private TerminfindungModel map(Terminfindung terminfindung) {

		TerminfindungModel terminfindungModel = beanMapper.map(terminfindung, TerminfindungModel.class);
		initialisierePraeferenzenFuerTeilnehmer(terminfindungModel);
		return terminfindungModel;	
	}

	/**
	 * Bildet einen View-Präferenzwert auf einen Persistenz-Präferenzwert ab.
	 * Die Abbildung ist trivial, sie dient aber dazu die Modelle wirksam zu entkoppeln.
	 * Das die numerische Codierung des Enums sowohl in der Datenbank abgelegt wird,
	 * als auch im GUI sichbar ist, scheint es sinnvoll, hier ein Mapping einzuführen,
	 * das sich nicht auf diese Codierung verlässt, sondern einzelne Werte explizit einander
	 * zuordnet. Auf eine Implementierung über Dozer wurde verzichtet, da hierfür
	 * ebenfalls eigener Mapping Code erforderlich wäre (Einzelne Enum-Werte können von Dozer
	 * standardmäßig nicht abgebildet werden.)
	 *
	 * @param praeferenzModel der View-Präferenzwert
	 * @return der Persistenz-Präferenzwert
	 * @throws TerminfindungBusinessException Wird bei unbekannten View-Präferenzewerten erzeugt
	 */
	private Praeferenz map (PraeferenzModel praeferenzModel) throws TerminfindungBusinessException{

		switch (praeferenzModel) {
			case JA: return Praeferenz.JA;
			case NEIN : return Praeferenz.NEIN;
			case WENN_ES_SEIN_MUSS : return Praeferenz.WENN_ES_SEIN_MUSS;
			default: throw new TerminfindungBusinessException(FehlerSchluessel.MSG_PARAMETER_UNGUELTIG);
		}
	}

	/**
	 * Stellt die Rückwärtsverkettung von Teilnehmern zu ihren Praeferenzen her.
	 * TODO: Das sollte im Persistenzmodell abgebildet werden, dann kann diese
	 * Methode entfallen.
	 * 
	 * @param vtf Die Terminfindung
	 */
	private void initialisierePraeferenzenFuerTeilnehmer(TerminfindungModel vtf) {
		
		for (ZeitraumModel zeitraum : vtf.getAlleZeitraeume()) {
			for (TeilnehmerZeitraumModel vtz : zeitraum.getTeilnehmerZeitraeume()) {
				vtz.getTeilnehmer().getPraeferenzen().add(vtz);
			}
		}
	}

	/*--------------------------------------------------------------------------
	 *  Getter und Setter
	 *--------------------------------------------------------------------------*/
	
	public Teilnahme getTeilnahme() {
		return teilnahme;
	}

	public void setTeilnahme(Teilnahme teilnahme) {
		this.teilnahme = teilnahme;
	}

	public Mapper getBeanMapper() {
		return beanMapper;
	}

	public void setBeanMapper(Mapper beanMapper) {
		this.beanMapper = beanMapper;
	}

	public Verwaltung getVerwaltung() {
		return verwaltung;
	}

	public void setVerwaltung(Verwaltung verwaltung) {
		this.verwaltung = verwaltung;
	}
	
	public Erstellung getErstellung() {
		return erstellung;
	}

	public void setErstellung(Erstellung erstellung) {
		this.erstellung = erstellung;
	}
}
