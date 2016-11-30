package com.portofrotterdam.versiondebt;

import com.thoughtworks.xstream.XStream;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;

public class VersiondebtsFactory {

	private XStream stream;

	private VersiondebtsFactory(final ClassLoader classLoader) {
		stream = new XStream();
		stream.setClassLoader(classLoader);
		stream.processAnnotations(new Class[]{
				Versiondebts.class,
				Versiondebts.VersiondebtItem.class,
				Versiondebts.VersiondebtItem.Version.class});
	}

	public static VersiondebtsFactory newInstance() {
		return new VersiondebtsFactory(Versiondebts.class.getClassLoader());
	}

	public Versiondebts fromXML(final String xml) {
		return (Versiondebts) stream.fromXML(xml);
	}

	public Versiondebts fromXML(final InputStream inputStream) {
		return (Versiondebts) stream.fromXML(inputStream);
	}

	public void toXML(final Object object, final OutputStream out) {
		stream.toXML(object, out);
	}

	public void toXML(final Object object, final Writer out) {
		stream.toXML(object, out);
	}
}
