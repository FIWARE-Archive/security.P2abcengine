function csrf() {
	var links = document.getElementsByTagName('a');
    var cv = readCookie('csrf');
    for(i = 0; i < links.length; i++) {
		links[i].href += "?csrf=" + cv;
	}
	var forms = document.getElementsByTagName('form');
    for(i = 0; i < forms.length; i++) {
		forms[i].innerHTML += '<input type="hidden" name="csrf" value="' + cv + '"/>';
	}
}

function readCookie(name) {
    name += '=';
    for (var ca = document.cookie.split(/;\s*/), i = ca.length - 1; i >= 0; i--)
        if (!ca[i].indexOf(name))
            return ca[i].replace(name, '');
}
