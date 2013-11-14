package com.mediaworx.opencms.intellijconnector;

import org.dom4j.Element;
import org.opencms.main.OpenCms;
import org.opencms.module.CmsModule;
import org.opencms.module.CmsModuleXmlHandler;

public class MetaXmlHelper {

	public Element getModuleXml(String moduleName) {
		CmsModule module = OpenCms.getModuleManager().getModule(moduleName);
		if (module == null) {
			throw new IllegalArgumentException(moduleName + " is not a valid OpenCms module");
		}
		return CmsModuleXmlHandler.generateXml(module);
	}


}
