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

package org.eclipse.birt.report.model.elements;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.birt.report.model.api.ListingHandle;
import org.eclipse.birt.report.model.api.command.ContentException;
import org.eclipse.birt.report.model.api.elements.ReportDesignConstants;
import org.eclipse.birt.report.model.api.metadata.IElementDefn;
import org.eclipse.birt.report.model.api.util.StringUtil;
import org.eclipse.birt.report.model.api.validators.DataSetRequiredValidator;
import org.eclipse.birt.report.model.api.validators.GroupNameValidator;
import org.eclipse.birt.report.model.core.BackRef;
import org.eclipse.birt.report.model.core.ContainerContext;
import org.eclipse.birt.report.model.core.ContainerSlot;
import org.eclipse.birt.report.model.core.DesignElement;
import org.eclipse.birt.report.model.core.Module;
import org.eclipse.birt.report.model.elements.interfaces.IDesignElementModel;
import org.eclipse.birt.report.model.elements.interfaces.IGroupElementModel;
import org.eclipse.birt.report.model.elements.interfaces.IListingElementModel;
import org.eclipse.birt.report.model.elements.interfaces.IReportItemModel;
import org.eclipse.birt.report.model.metadata.ElementPropertyDefn;
import org.eclipse.birt.report.model.metadata.MetaDataDictionary;
import org.eclipse.birt.report.model.util.ContentExceptionFactory;
import org.eclipse.birt.report.model.util.ModelUtil;
import org.eclipse.birt.report.model.util.VersionControlMgr;
import org.eclipse.birt.report.model.util.VersionUtil;

/**
 * This class represents the properties and slots common to the List and Table
 * elements.
 * 
 */

public abstract class ListingElement extends ReportItem
		implements
			IListingElementModel
{

	/**
	 * Default value of page break interval.
	 */
	private static final Integer PAGE_BREAK_INTERVAL_DEFAULT_VALUE = new Integer(
			50 );

	/**
	 * Default constructor.
	 */

	public ListingElement( )
	{
		super( );
		initSlots( );
	}

	/**
	 * Constructs the listing element with an optional name.
	 * 
	 * @param theName
	 *            the optional name
	 */

	public ListingElement( String theName )
	{
		super( theName );
		initSlots( );
	}

	/**
	 * Gets the list of groups. Groups are in order from the outer-most (most
	 * general) group to the inner-most (most specific) group.
	 * <p>
	 * The application MUST NOT modify this list. Use the handle class to make
	 * modifications.
	 * 
	 * @return the list of groups. The list contains <code>ListingGroup</code>
	 *         elements.
	 */

	public List getGroups( )
	{
		return slots[GROUP_SLOT].getContents( );
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.birt.report.model.core.DesignElement#getSlot(int)
	 */

	public ContainerSlot getSlot( int slot )
	{
		assert slot >= 0 && slot < getDefn( ).getSlotCount( );
		return slots[slot];
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.birt.report.model.core.DesignElement#getDisplayLabel(org.eclipse.birt.report.model.elements.ReportDesign,
	 *      int)
	 */

	public String getDisplayLabel( Module module, int level )
	{
		String displayLabel = super.getDisplayLabel( module, level );
		if ( level == IDesignElementModel.FULL_LABEL )
		{
			String name = getStringProperty( module,
					IReportItemModel.DATA_SET_PROP );
			name = limitStringLength( name );
			if ( !StringUtil.isBlank( name ) )
			{
				displayLabel += "(" + name + ")"; //$NON-NLS-1$//$NON-NLS-2$
			}
		}
		return displayLabel;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.birt.report.model.core.DesignElement#validate(org.eclipse.birt.report.model.elements.ReportDesign)
	 */

	public List validate( Module module )
	{
		List list = super.validate( module );

		list.addAll( validateStructureList( module, SORT_PROP ) );
		list.addAll( validateStructureList( module, FILTER_PROP ) );

		// Check whether this table/list has data set or its List/Table
		// container has data set.

		if ( getDataSetElement( module ) == null )
		{
			list.addAll( DataSetRequiredValidator.getInstance( ).validate(
					module, this ) );
		}
		else
		{
			// do the check of the group name

			list.addAll( GroupNameValidator.getInstance( ).validate( module,
					this ) );
		}

		return list;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.birt.report.model.core.DesignElement#checkContent(org.eclipse.birt.report.model.core.Module,
	 *      org.eclipse.birt.report.model.core.ContainerInfo,
	 *      org.eclipse.birt.report.model.api.metadata.IElementDefn)
	 */

	public List checkContent( Module module, ContainerContext containerInfo,
			IElementDefn defn )
	{
		List errors = super.checkContent( module, containerInfo, defn );
		if ( !errors.isEmpty( ) )
			return errors;

		// do the check of the group name

		if ( defn.isKindOf( MetaDataDictionary.getInstance( ).getElement(
				ReportDesignConstants.GROUP_ELEMENT ) ) )
		{
			// check the data binding reference

			if ( isDataBindingReferring( module ) )
			{
				errors
						.add( ContentExceptionFactory
								.createContentException(
										containerInfo,
										null,
										ContentException.DESIGN_EXCEPTION_GROUPS_CHANGE_FORBIDDEN ) );
				return errors;
			}
		}

		return errors;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.birt.report.model.core.DesignElement#checkContent(org.eclipse.birt.report.model.core.Module,
	 *      org.eclipse.birt.report.model.core.ContainerInfo,
	 *      org.eclipse.birt.report.model.core.DesignElement)
	 */

	public List checkContent( Module module, ContainerContext containerInfo,
			DesignElement content )
	{
		List errors = super.checkContent( module, containerInfo, content );
		if ( !errors.isEmpty( ) )
			return errors;

		// do the check of the group name

		if ( content instanceof GroupElement )
		{
			// check the data binding reference

			if ( isDataBindingReferring( module ) )
			{
				errors
						.add( ContentExceptionFactory
								.createContentException(
										containerInfo,
										content,
										ContentException.DESIGN_EXCEPTION_GROUPS_CHANGE_FORBIDDEN ) );
				return errors;
			}

			String checkedName = (String) content.getLocalProperty( module,
					IGroupElementModel.GROUP_NAME_PROP );
			if ( StringUtil.isBlank( checkedName ) )
				return errors;

			errors.addAll( GroupNameValidator.getInstance( )
					.validateForAddingGroup(
							(ListingHandle) getHandle( module ), checkedName ) );
		}

		return errors;
	}

	/**
	 * Returns listing elements that refers to this listing element directly.
	 * 
	 * @param module
	 *            the root of the listing element
	 * 
	 * @return a list containing listing elements.
	 */

	public List findReferredListingElements( Module module )
	{
		List returnList = new ArrayList( );

		List clients = getClientList( );
		for ( int i = 0; i < clients.size( ); i++ )
		{
			BackRef ref = (BackRef) clients.get( i );
			DesignElement refElement = ref.getElement( );
			if ( !IReportItemModel.DATA_BINDING_REF_PROP.equalsIgnoreCase( ref
					.getPropertyName( ) ) )
				continue;

			if ( !ModelUtil.isCompatibleDataBindingElements( this, refElement ) )
				continue;

			returnList.add( refElement );
		}

		return returnList;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.birt.report.model.core.DesignElement#getProperty(org.eclipse.birt.report.model.core.Module,
	 *      org.eclipse.birt.report.model.metadata.ElementPropertyDefn)
	 */
	public Object getProperty( Module module, ElementPropertyDefn prop )
	{
		if ( module == null
				|| ( !IListingElementModel.PAGE_BREAK_INTERVAL_PROP
						.equalsIgnoreCase( prop.getName( ) ) ) )
		{
			return super.getProperty( module, prop );
		}

		String version = module.getVersionManager( ).getVersion( );
		if ( version == null )
			return super.getProperty( module, prop );
		int versionNo = VersionUtil.parseVersion( version );
		if ( versionNo > VersionUtil.VERSION_3_2_15 )
		{
			return super.getProperty( module, prop );
		}

		Object value = getStrategy( ).getPropertyExceptRomDefault( module,
				this, prop );
		if ( value != null )
		{
			// As for the pageBreakInterval property, the method
			// updateContainerForContentElement is not necessary.
			return value;
		}

		return PAGE_BREAK_INTERVAL_DEFAULT_VALUE;

	}
}