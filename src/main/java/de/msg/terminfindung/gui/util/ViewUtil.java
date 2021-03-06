package de.msg.terminfindung.gui.util;

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


import java.io.Serializable;

import org.springframework.webflow.context.ExternalContext;

import de.bund.bva.isyfact.logging.IsyLogger;
import de.bund.bva.isyfact.logging.IsyLoggerFactory;
import de.msg.terminfindung.common.konstanten.FehlerSchluessel;

/**
 * Hilfsfunktionen für die Views.
 *
 * @author msg systems ag, Dirk Jäger
 */

public class ViewUtil implements Serializable{
	
	private static final long serialVersionUID = 1L;

	private static final IsyLogger LOG = IsyLoggerFactory.getLogger(ViewUtil.class);


    /**
     * Liest einen Request-Parameter aus der URL und gibt dessen Wert zurück
     **
     * @param name  der Namen des Request-Parameters
     * @param context der Spring Request Context
     */
    @Deprecated
    public static String getRequestParameter(String name, ExternalContext context) {

        String param = context.getRequestParameterMap().get(name);

        if (param == null) {

            LOG.warn(FehlerSchluessel.MSG_ALLGEMEINER_TECHNISCHER_FEHLER, "Requestparameter {} konnte nicht gelesen werden.", name);
        }
        return param;
    }

	/**
	 * Liest einen Request-Parameter aus der URL, wandelt ihn in einen Long-Wert um
	 * und gibt diesen Wert zurück.
	 **
	 * @param name  der Namen des Request-Parameters
	 * @param context der Spring Request Context
	 */
    @Deprecated
	public static TFNumberHolder getRequestParameterAsLong(String name, ExternalContext context) {

		Long result =0L;
		String paramValueStr = getRequestParameter(name, context);
		try {
			result = Long.parseLong(paramValueStr);
		}
		catch (NumberFormatException e) {
			LOG.warn("Request Parameter " + name + " konnte nicht in Long konvertiert werden, gebe 0L zurück",e.getMessage());
		}
		return new TFNumberHolder(result);
	}

}
