/******************************************************************************
 * Copyright (c) 2000-2018 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titanium.markers.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.titan.designer.AST.ASTVisitor;
import org.eclipse.titan.designer.AST.IVisitableNode;
import org.eclipse.titan.designer.AST.Module;
import org.eclipse.titan.designer.parsers.GlobalParser;
import org.eclipse.titan.designer.parsers.ProjectSourceParser;
import org.eclipse.titanium.markers.handler.Marker;
import org.eclipse.titanium.markers.handler.MarkerHandler;
import org.eclipse.titanium.markers.spotters.BaseModuleCodeSmellSpotter;
import org.eclipse.titanium.markers.spotters.BaseProjectCodeSmellSpotter;

/**
 * The core controller class of the code smell module
 * <p>
 * The <code>Analyzer</code> is responsible for conducting the code smell
 * analysis of a ttcn3 project or a single module. This includes handling and
 * executing the code smell spotters, locking the project during the analysis to
 * prevent modification.
 * <p>
 * Analyzer instances are immutable, but (slightly) expensive to construct.
 * Instances are obtained via the builder facility (see {@link #builder()}).
 * <p>
 * For performance reasons the {@link #withAll()} and {@link #withPreference()}
 * methods can also be used to obtain <code>Analyzer</code> instances. These are
 * cached instances, updated on preference setting changes.
 *
 * @author poroszd
 *
 */
public class Analyzer {
	private final Map<Class<? extends IVisitableNode>, Set<BaseModuleCodeSmellSpotter>> actions;
	private final Set<BaseProjectCodeSmellSpotter> projectActions;

	Analyzer(final Map<Class<? extends IVisitableNode>, Set<BaseModuleCodeSmellSpotter>> actions, final Set<BaseProjectCodeSmellSpotter> projectActions) {
		this.actions = actions;
		this.projectActions = projectActions;
	}

	// TODO: Run spotters parallel in a thread-pool
	private class CodeSmellVisitor extends ASTVisitor {
		private final List<Marker> markers;

		public CodeSmellVisitor() {
			markers = new ArrayList<Marker>();
		}

		@Override
		public int visit(final IVisitableNode node) {
			final Set<BaseModuleCodeSmellSpotter> actionsOnNode = actions.get(node.getClass());
			if (actionsOnNode != null) {
				for (final BaseModuleCodeSmellSpotter spotter : actionsOnNode) {
					markers.addAll(spotter.checkNode(node));
				}
			}
			return V_CONTINUE;
		}
	}

	private List<Marker> internalAnalyzeModule(final Module module) {
		final CodeSmellVisitor v = new CodeSmellVisitor();
		synchronized (module.getProject()) {
			module.accept(v);
		}
		return v.markers;
	}

	private List<Marker> internalAnalyzeProject(final IProject project) {
		final List<Marker> markers = new ArrayList<Marker>();
		for (final BaseProjectCodeSmellSpotter spotter : projectActions) {
			List<Marker> ms;
			synchronized (project) {
				ms = spotter.checkProject(project);
			}
			markers.addAll(ms);
		}
		return markers;
	}

	/**
	 * Analyze a single module.
	 * <p>
	 * Executes the configured code smell spotters on the given module (and if
	 * the <code>Analyzer</code> uses project-scoped code smell spotters, then
	 * those are executed, too, on the project of the module). Locking the
	 * project to prevent modification of the AST is handled internally.
	 *
	 * @param monitor
	 *            shows progress and makes it interruptable
	 * @param module
	 *            the ttcn3 module to analyze
	 *
	 * @return the code smells found in the given module
	 */
	public MarkerHandler analyzeModule(final IProgressMonitor monitor, final Module module) {
		final SubMonitor progress = SubMonitor.convert(monitor, 100);
		final IResource res = module.getLocation().getFile();

		final Map<IResource, List<Marker>> markers = new HashMap<IResource, List<Marker>>();
		markers.put(res, internalAnalyzeModule(module));
		progress.worked(80);
		if (progress.isCanceled()) {
			throw new OperationCanceledException();
		}

		final IProject project = module.getProject();
		markers.put(project, internalAnalyzeProject(project));
		progress.worked(20);
		return new MarkerHandler(markers);
	}

	/**
	 * Analyze a whole project.
	 * <p>
	 * Executes the configured code smell spotters on the given project. Locking
	 * the project to prevent modification of the AST is handled internally.
	 *
	 * @param monitor
	 *            shows progress and makes it interruptable
	 * @param module
	 *            the ttcn3 project to analyze
	 *
	 * @return the code smells found in the given project
	 */
	public MarkerHandler analyzeProject(final IProgressMonitor monitor, final IProject project) {
		final ProjectSourceParser projectSourceParser = GlobalParser.getProjectSourceParser(project);
		final Set<String> knownModuleNames = projectSourceParser.getKnownModuleNames();
		final SubMonitor progress = SubMonitor.convert(monitor, 1 + knownModuleNames.size());
		progress.subTask("Project level analysis");
		final Map<IResource, List<Marker>> markers = new HashMap<IResource, List<Marker>>();
		markers.put(project, internalAnalyzeProject(project));
		progress.worked(1);
		for (final String moduleName : knownModuleNames) {
			if (progress.isCanceled()) {
				throw new OperationCanceledException();
			}
			final Module mod = projectSourceParser.getModuleByName(moduleName);
			progress.subTask("Analyzing module " + mod.getName());
			final IResource moduleResource = mod.getLocation().getFile();
			markers.put(moduleResource, internalAnalyzeModule(mod));
			progress.worked(1);
		}

		return new MarkerHandler(markers);
	}

	/**
	 * The factory method of {@link AnalyzerBuilder}s.
	 * <p>
	 * To obtain an <code>Analyzer</code> instance, one have to call this
	 * method, configure the builder appropriately and let the builder construct
	 * the <code>Analyzer</code> itself. This is cruical to ensure the
	 * immutability, thus thread-safety of this class.
	 *
	 * @return a new {@link AnalyzerBuilder} instance
	 */
	public static AnalyzerBuilder builder() {
		return new AnalyzerBuilder();
	}


}
