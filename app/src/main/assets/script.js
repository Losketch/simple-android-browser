(function() {
	if (window.__CUSTOM_SCRIPT_INJECTED__) return;
	window.__CUSTOM_SCRIPT_INJECTED__ = true;

	console.log('%c[INJECTION SUCCESS]', 
		'color: white; background: green; padding: 2px 5px; border-radius: 3px;',
		'Custom script loaded from: ' + location.href);

	const banner = document.createElement('div');
	Object.assign(banner.style, {
		position: 'fixed',
		top: '10px',
		right: '10px',
		backgroundColor: '#1F883D',
		color: 'white',
		padding: '8px 16px',
		borderRadius: '4px',
		zIndex: '999999',
		boxShadow: '0 2px 5px rgba(0,0,0,0.3)',
		fontSize: '14px',
		fontFamily: 'Arial, sans-serif'
	});
	banner.innerHTML = '✅ Script Injected: v1.0';

	setTimeout(() => {
		banner.style.opacity = '0';
		setTimeout(() => banner.remove(), 1000);
	}, 3000);

	document.documentElement.appendChild(banner);

	const event = new CustomEvent('CustomScriptInjected', {
		detail: { 
			version: '1.0',
			timestamp: Date.now()
		}
	});
	window.dispatchEvent(event);
})();