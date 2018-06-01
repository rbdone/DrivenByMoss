// Written by Jürgen Moßgraber - mossgrabers.de
// (c) 2017-2018
// Licensed under LGPLv3 - http://www.gnu.org/licenses/lgpl-3.0.txt

package de.mossgrabers.controller.kontrol.usb.mki;

import de.mossgrabers.framework.controller.DefaultControllerDefinition;
import de.mossgrabers.framework.usb.USBMatcher;
import de.mossgrabers.framework.utils.OperatingSystem;
import de.mossgrabers.framework.utils.Pair;

import java.util.Collections;
import java.util.List;
import java.util.UUID;


/**
 * Definition class for the Native Instruments Kontrol Mk I controller extension.
 *
 * @author J&uuml;rgen Mo&szlig;graber
 */
public class Kontrol1ControllerDefinition extends DefaultControllerDefinition
{
    private static final UUID []   EXTENSION_ID       =
    {
        UUID.fromString ("457ef1d3-d197-4a94-a1d0-b4322ecbdd7d"),
        UUID.fromString ("90817073-0c11-41cf-8c56-f3334ec91fc4"),
        UUID.fromString ("99ff3646-3a65-47e5-a0e2-58c1c1799e93"),
        UUID.fromString ("18d5c565-f496-406d-8c3f-5af1004f61ff")
    };

    private static final String [] HARDWARE_MODEL     =
    {
        "Komplete Kontrol S25 mk I",
        "Komplete Kontrol S49 mk I",
        "Komplete Kontrol S61 mk I",
        "Komplete Kontrol S88 mk I"
    };

    private static final short     VENDOR_ID          = 0x17cc;
    private static final short []  PRODUCT_ID         =
    {
        0x1340,
        0x1350,
        0x1360,
        0x1410
    };
    /** Komplete Kontrol USB Interface for the display. */
    private static final byte      INTERFACE_NUMBER   = 2;

    /** Komplete Kontrol USB display and button/knob/keys (HID) endpoint. */
    private static final byte []   ENDPOINT_ADDRESSES =
    {
        (byte) 2,
        (byte) 0x82
    };

    private short                  productID;


    /**
     * Constructor.
     *
     * @param modelIndex The index of the specific model (S25,
     */
    public Kontrol1ControllerDefinition (final int modelIndex)
    {
        super ("", "Jürgen Moßgraber", "1.00", EXTENSION_ID[modelIndex], HARDWARE_MODEL[modelIndex], "Native Instruments", 1, 0);
        this.productID = PRODUCT_ID[modelIndex];
    }


    /** {@inheritDoc} */
    @Override
    public List<Pair<String [], String []>> getMidiDiscoveryPairs (final OperatingSystem os)
    {
        return Collections.singletonList (this.addDeviceDiscoveryPair ("Komplete Kontrol - 1", null));
    }


    /** {@inheritDoc} */
    @Override
    public USBMatcher claimUSBDevice ()
    {
        return new USBMatcher (VENDOR_ID, this.productID, INTERFACE_NUMBER, ENDPOINT_ADDRESSES, new boolean []
        {
            false,
            false
        });
    }
}
