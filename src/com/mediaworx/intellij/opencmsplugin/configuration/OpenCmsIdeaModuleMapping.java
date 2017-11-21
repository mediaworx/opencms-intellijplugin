/*
 * This file is part of the OpenCms plugin for IntelliJ by mediaworx.
 *
 * For further information about the OpenCms plugin for IntelliJ, please
 * see the project website at GitHub:
 * https://github.com/mediaworx/opencms-intellijplugin
 *
 * Copyright (C) 2007-2016 mediaworx berlin AG (http://www.mediaworx.com)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation; either version 3 of the License, or (at your
 * option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.mediaworx.intellij.opencmsplugin.configuration;

import com.intellij.openapi.module.Module;
import org.jetbrains.annotations.NotNull;

/**
 * <p>opencms-intellijplugin - $RCSfile$</p>
 * <p>TODO: JavaDoc</p>
 * <p>
 * (c) 2017, mediaworx berlin AG
 * All rights reserved
 * </p>
 *
 * @author initial author: Kai Widmann - widmann@mediaworx.com, 21.11.17
 */
public class OpenCmsIdeaModuleMapping implements Comparable<OpenCmsIdeaModuleMapping> {
	private final Module ideaModule;
	private final OpenCmsModuleConfigurationData moduleConfiguration;

	public OpenCmsIdeaModuleMapping(Module ideaModule, OpenCmsModuleConfigurationData moduleConfiguration) {
		this.ideaModule = ideaModule;
		this.moduleConfiguration = moduleConfiguration;
	}

	public final Module getIdeaModule() {
		return ideaModule;
	}

	public final OpenCmsModuleConfigurationData getModuleConfiguration() {
		return moduleConfiguration;
	}

	@Override
	public int compareTo(@NotNull OpenCmsIdeaModuleMapping o) {
		return ideaModule.getName().compareTo(o.getIdeaModule().getName());
	}
}
