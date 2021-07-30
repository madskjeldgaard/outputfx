// First, a source sound for testing

// Now create the FXChain for the output
(
s.waitForBoot{

	// Create a main outputfx using the default fx that come with the quark
	m = OFX_OutputFX.new(
		server: s,
		numChannels: 2,
		slotNames: [\ofx_chorus, \ofx_ringmod1, \ofx_freqshift, \ofx_timemachine, \ofx_dfm1, \ofx_jpverb ],
		busIndex: 0
	);

	// Open up gui
	m.gui;

}
)
