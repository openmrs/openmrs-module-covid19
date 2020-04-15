/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.covid19;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Locale;

import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.openmrs.Concept;
import org.openmrs.ConceptAnswer;
import org.openmrs.ConceptDatatype;
import org.openmrs.ConceptName;
import org.openmrs.Encounter;
import org.openmrs.EncounterType;
import org.openmrs.api.ConceptService;
import org.openmrs.api.EncounterService;
import org.openmrs.api.FormService;
import org.openmrs.api.context.Context;
import org.openmrs.api.context.ServiceContext;
import org.openmrs.module.BaseModuleActivator;
import org.openmrs.module.htmlformentry.HtmlFormEntryService;
import org.openmrs.module.htmlformentryui.HtmlFormUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

/**
 * This class contains the logic that is run every time this module is either started or shutdown
 */
@Component
public class COVID19Activator extends BaseModuleActivator implements ApplicationContextAware {
	
	private static final Logger log = LoggerFactory.getLogger(COVID19Activator.class);
	
	private ApplicationContext applicationContext;
	
	@Autowired
	@Qualifier("conceptService")
	private ConceptService conceptService;
	
	@Autowired
	@Qualifier("encounterService")
	private EncounterService encounterService;
	
	@Autowired
	@Qualifier("formService")
	private FormService formService;
	
	private PathMatchingResourcePatternResolver patternResolver = new PathMatchingResourcePatternResolver();
	
	/**
	 * @see #started()
	 */
	@SneakyThrows
	public void started() {
		log.info("Started COVID-19");
		
		if (this.applicationContext == null) {
			Method getServiceContext = Context.class.getDeclaredMethod("getServiceContext");
			getServiceContext.setAccessible(true);
			ServiceContext serviceContext = (ServiceContext) getServiceContext.invoke(null);
			this.applicationContext = (ApplicationContext) FieldUtils.getField(ServiceContext.class, "applicationContext",
			    true).get(serviceContext);
		}
		
		this.applicationContext.getAutowireCapableBeanFactory().autowireBean(this);
		
		if (conceptService.getConceptByUuid("165903A") == null) {
			setupConcepts();
		}
		
		if (encounterService.getEncounterType("COVID-19 Screening") == null) {
			setupEncounters();
		}
		
		try {
			Class.forName(HtmlFormUtil.class.getName());
			importHtmlForms();
		}
		catch (ClassNotFoundException ignored) {}
	}
	
	private void importHtmlForms() throws IOException {
		HtmlFormEntryService htmlFormEntryService = Context.getService(HtmlFormEntryService.class);
		for (Resource resource : patternResolver.getResources("classpath:covid19/htmlforms/**/*.html")) {
			HtmlFormUtil.getHtmlFormFromResourceXml(formService, htmlFormEntryService,
			    IOUtils.toString(resource.getInputStream()));
		}
	}
	
	private void setupEncounters() {
		EncounterType screeningEncounter = new EncounterType();
		screeningEncounter.setUuid("0dbe80d4-e174-43f8-8636-e28e5d840034");
		screeningEncounter.setName("COVID-19 Screening");
		encounterService.saveEncounterType(screeningEncounter);
		
		EncounterType caseReportingEncounter = new EncounterType();
		caseReportingEncounter.setUuid("cfb13c00-ffcc-4e98-8bc0-60d50e7d34ee");
		caseReportingEncounter.setName("COVID-19 Case Reporting");
		encounterService.saveEncounterType(caseReportingEncounter);
	}
	
	private void setupConcepts() {
		// TODO These will eventually be replaced by CIEL
		Concept caseDiagnosed = new Concept();
		caseDiagnosed.setUuid("165903A");
		
		ConceptName caseDiagnosedName = new ConceptName();
		caseDiagnosedName.setLocale(Locale.ENGLISH);
		caseDiagnosedName.setLocalePreferred(true);
		caseDiagnosedName.setName("Country where diagnosed");
		
		ConceptDatatype coded = conceptService.getConceptDatatypeByName("Coded");
		
		caseDiagnosed.addName(caseDiagnosedName);
		caseDiagnosed.setConceptClass(conceptService.getConceptClassByName("Question"));
		caseDiagnosed.setDatatype(coded);
		
		caseDiagnosed.addAnswer(new ConceptAnswer(conceptService.getConceptByMapping("165820", "CIEL")));
		
		conceptService.saveConcept(caseDiagnosed);
		
		Concept concept = conceptService.getConceptByMapping("162689", "CIEL");
		concept.setDatatype(conceptService.getConceptDatatypeByName("Text"));
		conceptService.saveConcept(concept);
		
		for (String code : new String[] { "165198", "165655" }) {
			concept = conceptService.getConceptByMapping(code, "CIEL");
			concept.setDatatype(coded);
			conceptService.saveConcept(concept);
		}
		
		concept = conceptService.getConceptByMapping("165795", "CIEL");
		concept.setConceptClass(conceptService.getConceptClassByName("Finding"));
		concept.setDatatype(coded);
	}
	
	/**
	 * @see #shutdown()
	 */
	public void shutdown() {
		log.info("Shutdown COVID-19");
	}
	
	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}
}
