function basementUpdate( command )
{
	var httpObject1 = getHttpObject();
	if ( !httpObject1 )
	return true;

	isRefreshing = true;
	httpObject1.onreadystatechange = function()
	{
		if ( httpObject1.readyState != 4 )
			return;

		var bodyBegin = httpObject1.responseText.indexOf( ">", httpObject1.responseText.indexOf( "<body" ) ) + 1;
		var bodyEnd = httpObject1.responseText.indexOf( "</body>" );

		if ( bodyBegin > 0 && bodyEnd > 0 )
		{
			top.charpane.document.getElementsByTagName( "body" )[0].innerHTML =
				httpObject1.responseText.substring( bodyBegin, bodyEnd );
		}

		isRefreshing = false;
		document.location.href = "basement.php";
	}

	var selects = document.getElementsByTagName( "select" );
	for ( var i = 0; i < selects.length; ++i )
		selects[i].disabled = true;

	var buttons = document.getElementsByTagName( "input" );
	for ( var i = 0; i < buttons.length; ++i )
		buttons[i].disabled = true;

	httpObject1.open( "POST", "/KoLmafia/sideCommand?cmd=" + command, true );
	httpObject1.send( "" );
}


function changeBasementGear()
{
	var select = document.getElementById( "gear" );
	if ( select.selectedIndex != 0 )
		basementUpdate( select.options[select.selectedIndex].value );
}


function changeBasementEffects()
{
	var command = "";
	var select = document.getElementById( "potion" );

	var current;

	for ( var i = 0; i < select.options.length; ++i )
	{
		if ( select.options[i].selected )
		{
			current = select.options[i].innerHTML;
			current = current.substring( 0, current.indexOf( " (" ) );
			command += current + "; ";
		}
	}

	basementUpdate( command );
}


function computeNetBoost( initial )
{
	var boost = 0;
	var select = document.getElementById( "potion" );

	for ( var i = 0; i < select.options.length; ++i )
		if ( select.options[i].selected )
			boost += 1 * select.options[i].value;

	var changeup = getObject( "changeup" );
	changeup.innerHTML = "" + (initial + boost);

	if ( boost == 0 )
		changeup.style.color = "black";
	else
		changeup.style.color = "blue";
}


function runBasementScript()
{	basementUpdate( "divehelp" );
}