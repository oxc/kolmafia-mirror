/**
 * Copyright (c) 2005-2007, KoLmafia development team
 * http://kolmafia.sourceforge.net/
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  [1] Redistributions of source code must retain the above copyright
 *      notice, this list of conditions and the following disclaimer.
 *  [2] Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in
 *      the documentation and/or other materials provided with the
 *      distribution.
 *  [3] Neither the name "KoLmafia" nor the names of its contributors may
 *      be used to endorse or promote products derived from this software
 *      without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package net.sourceforge.kolmafia;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class SendMessageRequest extends KoLRequest
{
	public static final Pattern ITEMID_PATTERN = Pattern.compile( "item[^=]*\\d*=(\\d+)" );
	public static final Pattern HOWMANY_PATTERN = Pattern.compile( "howmany\\d*=(\\d+)" );
	public static final Pattern QUANTITY_PATTERN = Pattern.compile( "quantity\\d*=([\\d,]+)" );
	public static final Pattern RECIPIENT_PATTERN = Pattern.compile( "towho=([^=&]+)" );

	private static boolean hadSendMessageFailure = false;
	private static boolean updateDisplayOnFailure = true;

	public Object [] attachments;
	public List source = inventory;
	public List destination = new ArrayList();
	public boolean isSubInstance = false;

	public SendMessageRequest( String formSource )
	{
		super( formSource );
		addFormField( "pwd" );
		this.attachments = new Object[0];
	}

	public SendMessageRequest( String formSource, AdventureResult attachment )
	{
		this( formSource );

		this.attachments = new Object[1];
		this.attachments[0] = attachment;
	}

	public SendMessageRequest( String formSource, Object [] attachments )
	{
		this( formSource );
		this.attachments = attachments;
	}

	public void attachItem( AdventureResult item, int index )
	{
		String which, quantity;

		if ( getCapacity() > 1 )
		{
			which = getItemField() + index;
			quantity = getQuantityField() + index;
		}
		else if ( alwaysIndex() )
		{
			which = getItemField() + "1";
			quantity = getQuantityField() + "1";
		}
		else
		{
			which = getItemField();
			quantity = getQuantityField();
		}

		addFormField( which, String.valueOf( item.getItemId() ) );
		addFormField( quantity, String.valueOf( item.getCount() ) );
	}

	public boolean alwaysIndex()
	{	return false;
	}

	public abstract String getItemField();
	public abstract String getQuantityField();
	public abstract String getMeatField();

	public abstract int getCapacity();
	public abstract SendMessageRequest getSubInstance( Object [] attachments );
	public abstract String getSuccessMessage();
	public abstract String getStatusMessage();

	private void runSubInstances()
	{
		int capacity = getCapacity();
		ArrayList subinstances = new ArrayList();

		int index1 = 0;

		AdventureResult item = null;
		int availableCount;
		int meatAttachment = 0;

		ArrayList nextAttachments = new ArrayList();
		SendMessageRequest subinstance = null;

		boolean allowNoTrade = allowUntradeableTransfer();
		boolean allowNoGift = allowUngiftableTransfer();

		while ( index1 < attachments.length )
		{
			nextAttachments.clear();

			do
			{
				item = (AdventureResult) attachments[index1++];

				if ( item.getName().equals( AdventureResult.MEAT ) )
				{
					meatAttachment += item.getCount();
					continue;
				}

				if ( !allowNoGift && !TradeableItemDatabase.isGiftable( item.getItemId() ) )
					continue;

				else if ( !allowNoTrade && !TradeableItemDatabase.isTradeable( item.getItemId() ) )
					continue;

				availableCount = item.getCount( source );

				if ( availableCount > 0 )
				{
					if ( item.getCount() > availableCount )
						item = item.getInstance( availableCount );

					nextAttachments.add( item );
				}
			}
			while ( index1 < attachments.length && nextAttachments.size() < capacity );

			// For each broken-up request, you create a new sending request
			// which will create the appropriate data to post.

			if ( !KoLmafia.refusesContinue() && !nextAttachments.isEmpty() )
			{
				subinstance = getSubInstance( nextAttachments.toArray() );
				subinstance.isSubInstance = true;
				subinstances.add( subinstance );
			}
		}

		// Now that you've determined all the sub instances, run
		// all of them.

		SendMessageRequest [] requests = new SendMessageRequest[ subinstances.size() ];
		subinstances.toArray( requests );

		if ( requests.length > 1 )
		{
			if ( meatAttachment > 0 )
				requests[0].addFormField( getMeatField(), String.valueOf( meatAttachment ) );

			for ( int i = 0; i < requests.length; ++i )
			{
				KoLmafia.updateDisplay( getStatusMessage() + " (request " + (i+1) + " of " + requests.length + ")..." );
				requests[i].run();
			}
		}
		else if ( requests.length == 1 )
		{
			KoLmafia.updateDisplay( getStatusMessage() + "..." );
			requests[0].run();
		}
		else if ( meatAttachment > 0 )
		{
			KoLmafia.updateDisplay( getStatusMessage() + "..." );
			SendMessageRequest request = getSubInstance( new Object[0] );
			request.addFormField( getMeatField(), String.valueOf( meatAttachment ) );
			request.run();
		}
		else if ( subinstances.isEmpty() )
		{
			super.run();
		}
	}

	/**
	 * Runs the request.  Note that this does not report an error if it fails;
	 * it merely parses the results to see if any gains were made.
	 */

	public final void run()
	{
		// First, check to see how many attachments are to be
		// placed in the closet - if there's too many,
		// then you'll need to break up the request

		if ( attachments != null && attachments.length != 0 )
		{
			int capacity = getCapacity();

			if ( !isSubInstance )
			{
				runSubInstances();
				return;
			}

			if ( capacity > 1 )
			{
				for ( int i = 1; i <= attachments.length; ++i )
					if ( attachments[i-1] != null )
						attachItem( (AdventureResult) attachments[i-1], i );
			}
			else if ( capacity == 1 )
			{
				if ( attachments[0] != null )
					attachItem( (AdventureResult) attachments[0], 0 );
			}
		}

		// Once all the form fields are broken up, this
		// just calls the normal run method from KoLRequest
		// to execute the request.

		hadSendMessageFailure = false;
		super.run();
	}

	public void processResults()
	{
		// Make sure that the message was actually sent -
		// the person could have input an invalid player Id

		if ( tallyItemTransfer() && (getSuccessMessage().equals( "" ) || responseText.indexOf( getSuccessMessage() ) != -1) )
		{
			// With that done, theneeds to be updated
			// to note that the items were sent.

			for ( int i = 0; i < attachments.length; ++i )
			{
				if ( attachments[i] == null )
					continue;

				if ( source == inventory )
					StaticEntity.getClient().processResult( ((AdventureResult)attachments[i]).getNegation() );
				else
					AdventureResult.addResultToList( source, ((AdventureResult)attachments[i]).getNegation() );

				if ( destination == inventory )
					StaticEntity.getClient().processResult( (AdventureResult) attachments[i] );
				else
					AdventureResult.addResultToList( destination, (AdventureResult) attachments[i] );
			}
		}
		else
		{
			hadSendMessageFailure = true;
			if ( tallyItemTransfer() && willUpdateDisplayOnFailure() )
			{
				for ( int i = 0; i < attachments.length; ++i )
					KoLmafia.updateDisplay( ERROR_STATE, "Transfer failed for " + attachments[i].toString() );

				int totalMeat = StaticEntity.parseInt( getFormField( getMeatField() ) );
				if ( totalMeat != 0 )
					KoLmafia.updateDisplay( ERROR_STATE, "Transfer failed for " + totalMeat + " meat" );
			}
		}
	}

	public static boolean hadSendMessageFailure()
	{	return hadSendMessageFailure;
	}

	public static boolean willUpdateDisplayOnFailure()
	{	return updateDisplayOnFailure;
	}

	public static void setUpdateDisplayOnFailure( boolean shouldUpdate )
	{	updateDisplayOnFailure = shouldUpdate;
	}

	public boolean allowUngiftableTransfer()
	{	return false;
	}

	public boolean allowUntradeableTransfer()
	{	return true;
	}

	public boolean tallyItemTransfer()
	{	return true;
	}

	public static boolean registerRequest( String command, String urlString, List source, int defaultQuantity )
	{	return registerRequest( command, urlString, ITEMID_PATTERN, HOWMANY_PATTERN, source, defaultQuantity );
	}

	public static boolean registerRequest( String command, String urlString, Pattern itemPattern, Pattern quantityPattern, List source, int defaultQuantity )
	{
		ArrayList itemList = new ArrayList();
		StringBuffer itemListBuffer = new StringBuffer();

		Matcher itemMatcher = itemPattern.matcher( urlString );
		Matcher quantityMatcher = quantityPattern == null ? null : quantityPattern.matcher( urlString );

		while ( itemMatcher.find() && (quantityMatcher == null || quantityMatcher.find()) )
		{
			int itemId = StaticEntity.parseInt( itemMatcher.group(1) );
			String name = TradeableItemDatabase.getItemName( itemId );

			// One of the "select" options is a zero value for the item id field.
			// Trying to parse it generates an exception, so skip it for now.

			if ( name == null )
				continue;

			int quantity = quantityPattern == null ? defaultQuantity : StaticEntity.parseInt( quantityMatcher.group(1) );
			AdventureResult item = new AdventureResult( itemId, quantity );

			if ( quantity < 1 )
				quantity = quantity + item.getCount( source );

			if ( itemListBuffer.length() > 0 )
				itemListBuffer.append( ", " );

			itemList.add( item.getInstance( source == inventory ? 0 - quantity : quantity ) );

			itemListBuffer.append( quantity );
			itemListBuffer.append( " " );
			itemListBuffer.append( name );
		}

		Matcher recipientMatcher = RECIPIENT_PATTERN.matcher( urlString );
		if ( recipientMatcher.find() )
		{
			itemListBuffer.append( " to " );
			itemListBuffer.append( KoLmafia.getPlayerName( recipientMatcher.group(1) ) );
		}

		if ( itemList.isEmpty() )
			return true;

		KoLmafia.getSessionStream().println();
		KoLmafia.getSessionStream().println( command + " " + itemListBuffer.toString() );

		for ( int i = 0; i < itemList.size(); ++i )
			StaticEntity.getClient().processResult( (AdventureResult) itemList.get(i) );

		return true;
	}
}
