// Written by Jürgen Moßgraber - mossgrabers.de
// (c) 2017-2020
// Licensed under LGPLv3 - http://www.gnu.org/licenses/lgpl-3.0.txt

package de.mossgrabers.controller.maschine.view;

import de.mossgrabers.controller.maschine.controller.MaschineColorManager;
import de.mossgrabers.controller.maschine.controller.MaschineControlSurface;
import de.mossgrabers.framework.controller.ButtonID;
import de.mossgrabers.framework.controller.grid.IPadGrid;
import de.mossgrabers.framework.daw.DAWColor;
import de.mossgrabers.framework.daw.IModel;
import de.mossgrabers.framework.daw.ITrackBank;
import de.mossgrabers.framework.daw.data.ITrack;
import de.mossgrabers.framework.mode.AbstractMode;


/**
 * The Mute view.
 *
 * @author J&uuml;rgen Mo&szlig;graber
 */
public class MuteView extends BaseView
{
    /**
     * Constructor.
     *
     * @param surface The controller
     * @param model The model
     */
    public MuteView (final MaschineControlSurface surface, final IModel model)
    {
        super ("Mute", surface, model);
    }


    /** {@inheritDoc} */
    @Override
    protected void executeFunction (final int padIndex)
    {
        final ITrack track = this.model.getCurrentTrackBank ().getItem (padIndex);

        if (this.isButtonCombination (ButtonID.DUPLICATE))
        {
            track.duplicate ();
            return;
        }

        if (this.isButtonCombination (ButtonID.DELETE))
        {
            track.remove ();
            return;
        }

        track.toggleMute ();
    }


    /** {@inheritDoc} */
    @Override
    public void drawGrid ()
    {
        final IPadGrid padGrid = this.surface.getPadGrid ();

        final ITrackBank tb = this.model.getCurrentTrackBank ();
        for (int i = 0; i < 16; i++)
        {
            final ITrack item = tb.getItem (i);
            final int x = i % 4;
            final int y = 3 - i / 4;
            if (item.doesExist ())
            {
                if (item.isMute ())
                    padGrid.lightEx (x, y, MaschineColorManager.COLOR_GREY);
                else
                    padGrid.lightEx (x, y, DAWColor.getColorIndex (item.getColor ()));
            }
            else
                padGrid.lightEx (x, y, AbstractMode.BUTTON_COLOR_OFF);
        }
    }
}