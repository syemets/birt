/*******************************************************************************
 * Copyright (c) 2004 Actuate Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Actuate Corporation  - initial API and implementation
 *******************************************************************************/

package org.eclipse.birt.report.engine.executor;

import org.eclipse.birt.report.engine.content.IContent;
import org.eclipse.birt.report.engine.content.ILabelContent;
import org.eclipse.birt.report.engine.emitter.IContentEmitter;
import org.eclipse.birt.report.engine.ir.LabelItemDesign;
import org.eclipse.birt.report.engine.script.internal.LabelScriptExecutor;

/**
 * the labelItem excutor
 * 
 * @version $Revision: 1.20 $ $Date: 2006/06/22 08:38:23 $
 */
public class LabelItemExecutor extends QueryItemExecutor
{

	/**
	 * constructor
	 * 
	 * @param context
	 *            the excutor context
	 * @param visitor
	 *            the report executor visitor
	 */
	public LabelItemExecutor( ExecutorManager manager )
	{
		super( manager );
	}

	/**
	 * execute a label and output an label item content. The execution process
	 * is:
	 * <li> create an label
	 * <li> push it into the stack
	 * <li> intialize the content
	 * <li> process the action, bookmark, style ,visibility.
	 * <li> execute the onCreate if necessary
	 * <li> call emitter to start the label
	 * <li> popup the label.
	 * 
	 * @see org.eclipse.birt.report.engine.executor.ReportItemExcutor#execute(IContentEmitter)
	 */
	public IContent execute( )
	{
		LabelItemDesign labelDesign = (LabelItemDesign)getDesign();
		ILabelContent labelContent = report.createLabelContent( );
		setContent(labelContent);

		executeQuery();
		context.registerOnPageBreak( content );		
		
		initializeContent( labelDesign, labelContent );

		processAction( labelDesign, labelContent );
		processBookmark( labelDesign, labelContent );
		processStyle( labelDesign, labelContent );
		processVisibility( labelDesign, labelContent );

		if ( context.isInFactory( ) )
		{
			LabelScriptExecutor.handleOnCreate( labelContent,
					context );
		}

		startTOCEntry( labelContent );
		
		if (emitter != null)
		{
			emitter.startLabel( labelContent );
		}
		return labelContent;
	}
	
	public void close( )
	{
		context.unregisterOnPageBreak( content );
		finishTOCEntry( );
		closeQuery( );
		manager.releaseExecutor( ExecutorManager.LABELITEM, this );
	}
}