// First, a source sound for testing

// Now create the FXChain for the output
(
s.waitForBoot{

	OFX_Chain.loadDefaultChains();

	// Create a main outputfx with random chain
	m = OFX_OutputFX.new(
		server: s,
		numChannels: 2,
		slotNames: [\ofx_chorus, \ofx_timemachine, \ofx_dfm1, \ofx_jpverb ],
		busIndex: 0
	);

	// Open up gui
	m.gui;

}
)
