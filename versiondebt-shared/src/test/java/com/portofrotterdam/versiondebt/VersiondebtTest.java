package com.portofrotterdam.versiondebt;

import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;

public class VersiondebtTest {

	private VersiondebtsFactory versiondebtsFactory;

	@Before
	public void setUp() throws Exception {
		versiondebtsFactory = VersiondebtsFactory.newInstance();
	}

	@Test
	public void create() throws Exception {
		final Versiondebts versiondebts = versiondebtsFactory.fromXML(getClass().getResourceAsStream("/versiondebt.xml"));
		assertNotNull(versiondebts);
	}
}
