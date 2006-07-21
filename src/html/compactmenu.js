var isRefreshing = false;

// Standard http object retrieval to handle all the various
// modern browsers.

function getHttpObject()
{
    var httpObject = false;
    if ( window.ActiveXObject )
    {
    	try
    	{
    		httpObject = new ActiveXObject( "Msxml2.XMLHTTP" );
    	}
    	catch ( e )
    	{
    		httpObject = new ActiveXObject( "Microsoft.XMLHTTP" );
    	}
    }
    else
    	httpObject = new XMLHttpRequest();

    return httpObject;
};

function displayText( command, responseText )
{
	if ( responseText == null )
	{
		parent.location.href = "http://www.kingdomofloathing.com/login.php?results=Session timed out.";
		return true;
    }

    var display = top.chatpane.document.getElementById( "ChatWindow" );
    if ( !display )
    {
        if ( !top.chatpane.document.cmdform )
            top.chatpane.location = "http://<!--MAFIA_HOST_PORT-->/cli.html";

        return true;
    }

    if ( command != "" )
        display.innerHTML += "<br><font color=olive> &gt; " + command + "</font><br><br>";

	if ( responseText.length < 2 )
		return;

	display.innerHTML += responseText;

	if ( !isRefreshing && responseText.indexOf( "<!-- REFRESH -->" ) != -1 )
	{
		isRefreshing = true;
		top.charpane.location.reload( true );
		isRefreshing = false;
	}
}

// Steal the container used to display quick skill effects
// so that there's no scary stacking.

function submitCommand()
{
    var httpObject = getHttpObject();
    if ( !httpObject )
    	return true;

    var command = (top.chatpane.document.getElementById( "ChatWindow" ) ? "submitCommand" : "executeCommand") +
        "?cmd=" + URLEncode( top.menupane.document.gcli.scriptbar.value );

    // Object verified, so do the open, on ready state change,
    // and anonymous function definition.

    httpObject.open( "GET", "http://<!--MAFIA_HOST_PORT-->/KoLmafia/" + command );
    httpObject.onreadystatechange = function ()
    {
    	if ( httpObject.readyState != 4 )
    		return false;

        displayText( top.menupane.document.gcli.scriptbar.value, httpObject.responseText );
    }

    httpObject.send( null );
    return true;
};

// ====================================================================
//			 URLEncode and URLDecode functions
//
// Copyright Albion Research Ltd. 2002
// httpObject://www.albionresearch.com/
//
// You may copy these functions providing that
// ( a ) you leave this copyright notice intact, and
// ( b ) if you use these functions on a publicly accessible
//		 web site you include a credit somewhere on the web site
//		 with a link back to http://www.albionresarch.com/
//
// If you find or fix any bugs, please let us know at albionresearch.com
//
// SpecialThanks to Neelesh Thakur for being the first to
// report a bug in URLDecode() - now fixed 2003-02-19.
// ====================================================================

function URLEncode( x )
{
	// The Javascript escape and unescape functions do not correspond
	// with what browsers actually do...
	var SAFECHARS = "0123456789" +					// Numeric
					"ABCDEFGHIJKLMNOPQRSTUVWXYZ" +	// Alphabetic
					"abcdefghijklmnopqrstuvwxyz" +
					"-_.!~*'()";					// RFC2396 Mark characters
	var HEX = "0123456789ABCDEF";

	var plaintext = x;
	var encoded = "";
	for ( var i = 0; i < plaintext.length; i++ ) {
		var ch = plaintext.charAt( i );
		if ( ch=="+" ) {
			encoded+="%2B";
		} else if ( ch == " " ) {
				encoded += "+";				// x-www-urlencoded, rather than %20
		} else if ( SAFECHARS.indexOf( ch ) != -1 ) {
				encoded += ch;
		} else {
				var charCode = ch.charCodeAt( 0 );
			if ( charCode > 255 ) {
					alert( "Unicode Character '" + ch + "' cannot be encoded using standard URL encoding.\n" +
								"( URL encoding only supports 8-bit characters. )\n" +
						"A space ( + ) will be substituted." );
				encoded += "+";
			} else {
				encoded += "%";
				encoded += HEX.charAt( ( charCode >> 4 ) & 0xF );
				encoded += HEX.charAt( charCode & 0xF );
			}
		}
	}

	return encoded;
};
