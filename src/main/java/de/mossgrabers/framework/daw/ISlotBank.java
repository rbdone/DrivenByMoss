// Written by Jürgen Moßgraber - mossgrabers.de
// (c) 2017-2020
// Licensed under LGPLv3 - http://www.gnu.org/licenses/lgpl-3.0.txt

package de.mossgrabers.framework.daw;

import de.mossgrabers.framework.daw.data.ISlot;


/**
 * Interface to a slot bank.
 *
 * @author J&uuml;rgen Mo&szlig;graber
 */
public interface ISlotBank extends IBank<ISlot>
{
    /**
     * Returns the first empty slot in the current clip window. If none is empty null is returned.
     * If startFrom is set the search starts from the given index (and wraps around after the last
     * one to 0).
     *
     * @param startFrom At what index to start the search
     * @return The empty slot or null if none is found
     */
    ISlot getEmptySlot (final int startFrom);
}