// Written by Jürgen Moßgraber - mossgrabers.de
// (c) 2017-2020
// Licensed under LGPLv3 - http://www.gnu.org/licenses/lgpl-3.0.txt

package de.mossgrabers.controller.sl.view;

import de.mossgrabers.controller.sl.SLConfiguration;
import de.mossgrabers.controller.sl.command.trigger.ButtonRowSelectCommand;
import de.mossgrabers.controller.sl.command.trigger.P2ButtonCommand;
import de.mossgrabers.controller.sl.controller.SLControlSurface;
import de.mossgrabers.controller.sl.mode.device.DeviceParamsMode;
import de.mossgrabers.framework.controller.ButtonID;
import de.mossgrabers.framework.daw.ICursorDevice;
import de.mossgrabers.framework.daw.IModel;
import de.mossgrabers.framework.daw.INoteClip;
import de.mossgrabers.framework.daw.ITrackBank;
import de.mossgrabers.framework.daw.ITransport;
import de.mossgrabers.framework.daw.constants.Resolution;
import de.mossgrabers.framework.daw.data.IChannel;
import de.mossgrabers.framework.daw.data.ITrack;
import de.mossgrabers.framework.mode.ModeManager;
import de.mossgrabers.framework.mode.Modes;
import de.mossgrabers.framework.utils.ButtonEvent;
import de.mossgrabers.framework.view.AbstractSequencerView;
import de.mossgrabers.framework.view.Views;

import java.util.Arrays;


/**
 * The view for playing and sequencing.
 *
 * @author J&uuml;rgen Mo&szlig;graber
 */
public class PlayView extends AbstractSequencerView<SLControlSurface, SLConfiguration> implements SLView
{
    private static final int NUM_DISPLAY_COLS = 16;
    private static final int NOTE_VELOCITY    = 127;

    private int              selectedPad;
    private boolean          isPlayMode;
    protected int []         pressedKeys;
    private TransportControl transportControl;


    /**
     * Constructor.
     *
     * @param surface The surface
     * @param model The model
     */
    public PlayView (final SLControlSurface surface, final IModel model)
    {
        super ("Play", surface, model, 128, NUM_DISPLAY_COLS);

        this.transportControl = new TransportControl (surface, model);

        this.selectedPad = 0;

        this.pressedKeys = new int [128];
        Arrays.fill (this.pressedKeys, 0);

        this.isPlayMode = true;

        final ITrackBank tb = model.getTrackBank ();
        tb.addSelectionObserver ( (index, isSelected) -> this.clearPressedKeys ());
        tb.addNoteObserver (this::updateNote);
    }


    /** {@inheritDoc} */
    @Override
    public void onButtonRow1 (final int index, final ButtonEvent event)
    {
        if (event != ButtonEvent.DOWN)
            return;

        final ModeManager modeManager = this.surface.getModeManager ();
        final Modes activeModeId = modeManager.getActiveOrTempModeId ();
        if (Modes.VIEW_SELECT == activeModeId)
        {
            if (index == 0)
            {
                this.surface.getViewManager ().setActiveView (Views.CONTROL);
                if (Modes.VOLUME.equals (modeManager.getPreviousModeId ()))
                    modeManager.restoreMode ();
                else
                    modeManager.setActiveMode (Modes.TRACK);
            }
            else
                modeManager.restoreMode ();
            this.surface.turnOffTransport ();
            return;
        }

        if (!Modes.SESSION.equals (activeModeId))
            modeManager.setActiveMode (Modes.SESSION);

        this.model.getSceneBank ().getItem (index).launch ();
    }


    /** {@inheritDoc} */
    @Override
    public void onButtonRow2 (final int index, final ButtonEvent event)
    {
        if (event != ButtonEvent.DOWN)
            return;

        final ModeManager modeManager = this.surface.getModeManager ();
        final Modes cm = modeManager.getActiveOrTempModeId ();
        if (!Modes.PLAY_OPTIONS.equals (cm))
            modeManager.setActiveMode (Modes.PLAY_OPTIONS);

        switch (index)
        {
            // Down
            case 0:
                this.clearPressedKeys ();
                this.scales.decDrumOctave ();
                this.model.getInstrumentDevice ().getDrumPadBank ().selectPreviousPage ();
                this.updateNoteMapping ();
                this.surface.getDisplay ().notify (this.scales.getDrumRangeText ());
                break;

            // Up
            case 1:
                this.clearPressedKeys ();
                this.scales.incDrumOctave ();
                this.model.getInstrumentDevice ().getDrumPadBank ().selectNextPage ();
                this.updateNoteMapping ();
                this.surface.getDisplay ().notify (this.scales.getDrumRangeText ());
                break;

            case 2:
                this.changeResolution (0);
                this.surface.getDisplay ().notify (Resolution.getNameAt (this.selectedResolutionIndex));
                break;

            case 3:
                this.changeResolution (127);
                this.surface.getDisplay ().notify (Resolution.getNameAt (this.selectedResolutionIndex));
                break;

            case 4:
                this.changeScrollPosition (0);
                break;

            case 5:
                this.changeScrollPosition (127);
                break;

            case 6:
                break;

            // Toggle play / sequencer
            case 7:
                this.isPlayMode = !this.isPlayMode;
                this.surface.getDisplay ().notify (this.isPlayMode ? "Play/Select" : "Sequence");
                this.updateNoteMapping ();
                break;

            default:
                // Intentionally empty
                break;
        }
    }


    /** {@inheritDoc} */
    @Override
    public void onButtonRow3 (final int index, final ButtonEvent event)
    {
        if (event == ButtonEvent.DOWN)
            this.onSeqGridNote (44 + index, PlayView.NOTE_VELOCITY);
        else if (event == ButtonEvent.UP)
            this.onSeqGridNote (44 + index, 0);
    }


    /** {@inheritDoc} */
    @Override
    public void onButtonRow4 (final int index, final ButtonEvent event)
    {
        if (event == ButtonEvent.DOWN)
            this.onSeqGridNote (36 + index, PlayView.NOTE_VELOCITY);
        else if (event == ButtonEvent.UP)
            this.onSeqGridNote (36 + index, 0);
    }


    /** {@inheritDoc} */
    @Override
    public void onButtonRow5 (final int index, final ButtonEvent event)
    {
        this.transportControl.execute (index, event);
    }


    /** {@inheritDoc} */
    @Override
    public void onButtonRow1Select ()
    {
        this.surface.getModeManager ().setActiveMode (Modes.SESSION);
    }


    /** {@inheritDoc} */
    @Override
    public void onButtonRow2Select ()
    {
        this.surface.getModeManager ().setActiveMode (Modes.PLAY_OPTIONS);
    }


    /** {@inheritDoc} */
    @Override
    public void onButtonP1 (final boolean isUp, final ButtonEvent event)
    {
        if (event != ButtonEvent.DOWN)
            return;

        final Modes activeModeId = this.surface.getModeManager ().getActiveOrTempModeId ();
        if (Modes.SESSION == activeModeId)
        {
            if (isUp)
                this.model.getSceneBank ().selectNextPage ();
            else
                this.model.getSceneBank ().selectPreviousPage ();
            return;
        }

        if (Modes.VOLUME.equals (activeModeId))
        {
            new P2ButtonCommand (isUp, this.model, this.surface).execute (ButtonEvent.DOWN, 127);
            return;
        }

        if (Modes.TRACK.equals (activeModeId) || Modes.MASTER.equals (activeModeId))
        {
            new ButtonRowSelectCommand<> (3, this.model, this.surface).execute (ButtonEvent.DOWN, 127);
            return;
        }

        if (Modes.PLAY_OPTIONS.equals (activeModeId))
            return;

        final DeviceParamsMode mode = (DeviceParamsMode) this.surface.getModeManager ().getMode (Modes.DEVICE_PARAMS);
        if (isUp)
            mode.nextPage ();
        else
            mode.previousPage ();
    }


    /** {@inheritDoc} */
    @Override
    public int getButtonColor (final ButtonID buttonID)
    {
        final int buttonIDOrdinal = buttonID.ordinal ();

        // Button row 1: Launch Scene
        if (buttonIDOrdinal >= ButtonID.ROW1_1.ordinal () && buttonIDOrdinal <= ButtonID.ROW1_8.ordinal ())
            return SLControlSurface.MKII_BUTTON_STATE_OFF;

        // Button row 2: Track toggles
        if (buttonIDOrdinal >= ButtonID.ROW2_1.ordinal () && buttonIDOrdinal <= ButtonID.ROW2_8.ordinal ())
            return SLControlSurface.MKII_BUTTON_STATE_OFF;

        // Transport buttons
        if (this.surface.isTransportActive ())
        {
            if (buttonIDOrdinal >= ButtonID.ROW3_1.ordinal () && buttonIDOrdinal <= ButtonID.ROW3_8.ordinal ())
                return SLControlSurface.MKII_BUTTON_STATE_OFF;

            final Modes mode = this.surface.getModeManager ().getActiveOrTempModeId ();
            final boolean isSession = Modes.SESSION == mode;
            final boolean isDevice = Modes.DEVICE_PARAMS == mode;
            final boolean isPlayOptions = Modes.PLAY_OPTIONS == mode;
            final boolean isTrack = Modes.TRACK == mode;
            final boolean isMaster = Modes.MASTER == mode;
            final boolean isVolume = Modes.VOLUME == mode;
            final ITransport transport = this.model.getTransport ();

            switch (buttonID)
            {
                case ROW4_3:
                    return !transport.isPlaying () ? SLControlSurface.MKII_BUTTON_STATE_ON : SLControlSurface.MKII_BUTTON_STATE_OFF;
                case ROW4_4:
                    return transport.isPlaying () ? SLControlSurface.MKII_BUTTON_STATE_ON : SLControlSurface.MKII_BUTTON_STATE_OFF;
                case ROW4_5:
                    return transport.isLoop () ? SLControlSurface.MKII_BUTTON_STATE_ON : SLControlSurface.MKII_BUTTON_STATE_OFF;
                case ROW4_6:
                    return transport.isRecording () ? SLControlSurface.MKII_BUTTON_STATE_ON : SLControlSurface.MKII_BUTTON_STATE_OFF;

                case ROW_SELECT_1:
                    return isSession ? SLControlSurface.MKII_BUTTON_STATE_ON : SLControlSurface.MKII_BUTTON_STATE_OFF;
                case ROW_SELECT_2:
                    return isDevice ? SLControlSurface.MKII_BUTTON_STATE_ON : SLControlSurface.MKII_BUTTON_STATE_OFF;
                case ROW_SELECT_3:
                    return isPlayOptions ? SLControlSurface.MKII_BUTTON_STATE_ON : SLControlSurface.MKII_BUTTON_STATE_OFF;
                case ROW_SELECT_4:
                    return isTrack || isMaster ? SLControlSurface.MKII_BUTTON_STATE_ON : SLControlSurface.MKII_BUTTON_STATE_OFF;
                case ROW_SELECT_6:
                    return isVolume ? SLControlSurface.MKII_BUTTON_STATE_ON : SLControlSurface.MKII_BUTTON_STATE_OFF;
                case ROW_SELECT_7:
                    return SLControlSurface.MKII_BUTTON_STATE_OFF;

                default:
                    return 0;
            }
        }

        // Draw sequencer
        return this.drawDrumGrid (buttonIDOrdinal);
    }


    /**
     * 'Draw' the drum grid sequencer.
     *
     * @param buttonIDOrdinal The ordinal number of the button
     * @return The colo for the step
     */
    public int drawDrumGrid (final int buttonIDOrdinal)
    {
        final boolean isRow3 = buttonIDOrdinal >= ButtonID.ROW3_1.ordinal () && buttonIDOrdinal <= ButtonID.ROW3_8.ordinal ();
        final int index = isRow3 ? buttonIDOrdinal - ButtonID.ROW3_1.ordinal () : buttonIDOrdinal - ButtonID.ROW4_1.ordinal ();
        final int x = index;
        final int y;
        if (this.isPlayMode)
            y = isRow3 ? 1 : 0;
        else
            y = isRow3 ? 0 : 1;
        final int col = 8 * y + x;

        if (this.isPlayMode)
        {
            final ICursorDevice primary = this.model.getInstrumentDevice ();
            final boolean isSoloed = primary.hasDrumPads () && primary.getDrumPadBank ().hasSoloedPads ();
            return this.getPadColor (col, primary, isSoloed);
        }

        if (!this.isActive ())
            return SLControlSurface.MKII_BUTTON_STATE_OFF;

        final INoteClip clip = this.getClip ();
        final boolean exists = clip.doesExist ();
        // Paint the sequencer steps
        final int step = clip.getCurrentStep ();
        final int hiStep = this.isInXRange (step) ? step % PlayView.NUM_DISPLAY_COLS : -1;
        final int offsetY = this.scales.getDrumOffset ();
        final int editMidiChannel = this.configuration.getMidiEditChannel ();

        if (col >= PlayView.NUM_DISPLAY_COLS)
            return SLControlSurface.MKII_BUTTON_STATE_OFF;

        final int isSet = clip.getStep (editMidiChannel, col, offsetY + this.selectedPad).getState ();
        final boolean hilite = col == hiStep;
        return exists && (isSet > 0 || hilite) ? SLControlSurface.MKII_BUTTON_STATE_ON : SLControlSurface.MKII_BUTTON_STATE_OFF;
    }


    /**
     * Clear the pressed keys.
     */
    public void clearPressedKeys ()
    {
        for (int i = 0; i < 128; i++)
            this.pressedKeys[i] = 0;
    }


    /**
     * A button of the sequencer grid was pressed.
     *
     * @param note The note pressed
     * @param velocity The velocity
     */
    public void onSeqGridNote (final int note, final int velocity)
    {
        if (!this.model.canSelectedTrackHoldNotes ())
            return;

        final int index = note - 36;

        final int offsetY = this.scales.getDrumOffset ();
        if (this.isPlayMode)
        {
            this.selectedPad = index; // 0-16

            // Mark selected note
            this.pressedKeys[offsetY + this.selectedPad] = velocity;

            this.surface.sendMidiEvent (0x90, this.keyManager.map (note), velocity);
        }
        else
        {
            if (velocity != 0)
                this.getClip ().toggleStep (this.configuration.getMidiEditChannel (), index < 8 ? index + 8 : index - 8, offsetY + this.selectedPad, this.configuration.isAccentActive () ? this.configuration.getFixedAccentValue () : velocity);
        }
    }


    /** {@inheritDoc} */
    @Override
    public void updateNoteMapping ()
    {
        this.delayedUpdateNoteMapping (this.model.canSelectedTrackHoldNotes () && this.isPlayMode ? this.scales.getDrumMatrix () : EMPTY_TABLE);
    }


    private int getPadColor (final int index, final ICursorDevice primary, final boolean isSoloed)
    {
        final int offsetY = this.scales.getDrumOffset ();
        // Playing note?
        if (this.pressedKeys[offsetY + index] > 0)
            return SLControlSurface.MKII_BUTTON_STATE_ON;
        // Selected?
        if (this.selectedPad == index)
            return SLControlSurface.MKII_BUTTON_STATE_ON;
        // Exists and active?
        final IChannel drumPad = primary.getDrumPadBank ().getItem (index);
        if (!drumPad.doesExist () || !drumPad.isActivated ())
            return SLControlSurface.MKII_BUTTON_STATE_OFF;
        // Muted or soloed?
        if (drumPad.isMute () || isSoloed && !drumPad.isSolo ())
            return SLControlSurface.MKII_BUTTON_STATE_OFF;
        return SLControlSurface.MKII_BUTTON_STATE_OFF;
    }


    private void changeScrollPosition (final int value)
    {
        final boolean isInc = value >= 65;
        if (isInc)
            this.getClip ().scrollStepsPageForward ();
        else
            this.getClip ().scrollStepsPageBackwards ();
    }


    private void changeResolution (final int value)
    {
        final boolean isInc = value >= 65;
        this.selectedResolutionIndex = Resolution.change (this.selectedResolutionIndex, isInc);
        this.getClip ().setStepLength (Resolution.getValueAt (this.selectedResolutionIndex));
    }


    /** {@inheritDoc} */
    @Override
    public void drawGrid ()
    {
        // Intentionally empty
    }


    /** {@inheritDoc} */
    @Override
    public void onGridNote (final int note, final int velocity)
    {
        // Intentionally empty
    }


    /**
     * The callback function for playing note changes.
     *
     * @param trackIndex The index of the track on which the note is playing
     * @param note The played note
     * @param velocity The played velocity
     */
    private void updateNote (final int trackIndex, final int note, final int velocity)
    {
        final ITrack sel = this.model.getCurrentTrackBank ().getSelectedItem ();
        if (sel != null && sel.getIndex () == trackIndex)
            this.pressedKeys[note] = velocity;
    }
}