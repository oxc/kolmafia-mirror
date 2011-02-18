/**
 *
 */

package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.KoLmafiaGUI;
import net.sourceforge.kolmafia.webui.RelayLoader;

public class WindowOpenCommand
	extends AbstractCommand
{
	public WindowOpenCommand()
	{
		this.usage = " - switch to tab or open window";
	}

	public void run( final String command, final String parameters )
	{
		if ( command.equals( "chat" ) )
		{
			KoLmafiaGUI.constructFrame( "ChatManager" );
			return;
		}

		if ( command.equals( "mail" ) )
		{
			KoLmafiaGUI.constructFrame( "MailboxFrame" );
			return;
		}

		if ( command.startsWith( "opt" ) )
		{
			KoLmafiaGUI.constructFrame( "OptionsFrame" );
			return;
		}

		if ( command.equals( "item" ) )
		{
			KoLmafiaGUI.constructFrame( "ItemManageFrame" );
			return;
		}

		if ( command.equals( "gear" ) )
		{
			KoLmafiaGUI.constructFrame( "GearChangeFrame" );
			return;
		}

		if ( command.equals( "radio" ) )
		{
			RelayLoader.launchRadioKoL();
			return;
		}
	}
}
