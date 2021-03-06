// Written by Jürgen Moßgraber - mossgrabers.de
// (c) 2017-2020
// Licensed under LGPLv3 - http://www.gnu.org/licenses/lgpl-3.0.txt

package de.mossgrabers.controller.maschine.view;

import de.mossgrabers.controller.maschine.MaschineConfiguration;
import de.mossgrabers.controller.maschine.controller.MaschineColorManager;
import de.mossgrabers.controller.maschine.controller.MaschineControlSurface;
import de.mossgrabers.framework.controller.ButtonID;
import de.mossgrabers.framework.controller.grid.IPadGrid;
import de.mossgrabers.framework.daw.DAWColor;
import de.mossgrabers.framework.daw.IModel;
import de.mossgrabers.framework.daw.ISlotBank;
import de.mossgrabers.framework.daw.data.ISlot;
import de.mossgrabers.framework.daw.data.ITrack;
import de.mossgrabers.framework.mode.AbstractMode;
import de.mossgrabers.framework.mode.BrowserActivator;
import de.mossgrabers.framework.mode.ModeManager;
import de.mossgrabers.framework.mode.Modes;
import de.mossgrabers.framework.utils.ButtonEvent;


/**
 * The Clip view.
 *
 * @author J&uuml;rgen Mo&szlig;graber
 */
public class ClipView extends BaseView
{
    private final BrowserActivator<MaschineControlSurface, MaschineConfiguration> browserModeActivator;


    /**
     * Constructor.
     *
     * @param surface The controller
     * @param model The model
     */
    public ClipView (final MaschineControlSurface surface, final IModel model)
    {
        super ("Clip", surface, model);

        this.browserModeActivator = new BrowserActivator<> (Modes.BROWSER, model, surface);
    }


    /** {@inheritDoc} */
    @Override
    protected void executeFunction (final int padIndex)
    {
        final ITrack track = this.model.getCurrentTrackBank ().getSelectedItem ();
        if (track == null)
            return;
        final ISlot slot = track.getSlotBank ().getItem (padIndex);

        final MaschineConfiguration configuration = this.surface.getConfiguration ();
        if (this.isButtonCombination (ButtonID.DUPLICATE))
        {
            if (track.doesExist ())
                slot.duplicate ();
            return;
        }

        // Stop clip
        if (this.isButtonCombination (ButtonID.STOP))
        {
            track.stop ();
            return;
        }

        // Browse for clips
        if (this.isButtonCombination (ButtonID.BROWSE))
        {
            if (!track.doesExist ())
                return;
            this.model.getBrowser ().replace (slot);
            final ModeManager modeManager = this.surface.getModeManager ();
            if (!modeManager.isActiveOrTempMode (Modes.BROWSER))
                this.browserModeActivator.activate ();
            return;
        }

        // Delete selected clip
        if (this.isButtonCombination (ButtonID.DELETE))
        {
            slot.remove ();
            return;
        }

        if (configuration.isSelectClipOnLaunch ())
            slot.select ();

        if (!track.isRecArm ())
        {
            slot.launch ();
            return;
        }

        if (slot.hasContent ())
        {
            slot.launch ();
            return;
        }

        switch (configuration.getActionForRecArmedPad ())
        {
            case 0:
                this.model.recordNoteClip (track, slot);
                break;

            case 1:
                final int lengthInBeats = configuration.getNewClipLenghthInBeats (this.model.getTransport ().getQuartersPerMeasure ());
                this.model.createNoteClip (track, slot, lengthInBeats, true);
                break;

            case 2:
            default:
                // Do nothing
                break;
        }
    }


    /** {@inheritDoc} */
    @Override
    public void onButton (final int index, final ButtonEvent event)
    {
        if (event != ButtonEvent.DOWN)
            return;

        final ITrack selectedTrack = this.model.getCurrentTrackBank ().getSelectedItem ();
        if (selectedTrack == null)
            return;

        final ISlotBank slotBank = selectedTrack.getSlotBank ();

        switch (index)
        {
            case 0:
                this.model.getCurrentTrackBank ().selectPreviousItem ();
                break;
            case 1:
                this.model.getCurrentTrackBank ().selectNextItem ();
                break;
            case 2:
                slotBank.selectPreviousPage ();
                break;
            case 3:
                slotBank.selectNextPage ();
                break;
            default:
                // Not used
                break;
        }
    }


    /** {@inheritDoc} */
    @Override
    public void drawGrid ()
    {
        final IPadGrid padGrid = this.surface.getPadGrid ();

        final ITrack selectedTrack = this.model.getCurrentTrackBank ().getSelectedItem ();
        if (selectedTrack == null)
            return;
        final ISlotBank slotBank = selectedTrack.getSlotBank ();
        for (int i = 0; i < 16; i++)
        {
            final ISlot item = slotBank.getItem (i);
            final int x = i % 4;
            final int y = 3 - i / 4;
            if (item.doesExist ())
            {
                if (item.isRecordingQueued ())
                    padGrid.lightEx (x, y, MaschineColorManager.COLOR_RED_LO);
                else if (item.isRecording ())
                    padGrid.lightEx (x, y, MaschineColorManager.COLOR_RED);
                else if (item.isPlayingQueued ())
                    padGrid.lightEx (x, y, MaschineColorManager.COLOR_GREEN_LO);
                else if (item.isPlaying ())
                    padGrid.lightEx (x, y, MaschineColorManager.COLOR_GREEN);
                else if (item.isStopQueued ())
                    padGrid.lightEx (x, y, MaschineColorManager.COLOR_GREEN_LO);
                else
                    padGrid.lightEx (x, y, DAWColor.getColorIndex (item.getColor ()));
            }
            else
                padGrid.lightEx (x, y, AbstractMode.BUTTON_COLOR_OFF);
        }
    }
}