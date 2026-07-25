package buildcraft.api.transport.pipe;

/**
 * Configuration event for the legacy RF pipe family, backed by Forge Energy
 * on Minecraft 1.20.1.
 */
public abstract class PipeEventRedstoneFlux extends PipeEvent {
    public final IFlowRedstoneFlux flow;

    protected PipeEventRedstoneFlux(IPipeHolder holder, IFlowRedstoneFlux flow) {
        super(holder);
        this.flow = flow;
    }

    public static final class Configure extends PipeEventRedstoneFlux {
        private int maxPower = 100;
        private boolean receiver;
        private boolean transferDisabled;

        public Configure(IPipeHolder holder, IFlowRedstoneFlux flow) {
            super(holder, flow);
        }

        public int getMaxPower() {
            return maxPower;
        }

        public void setMaxPower(int maxPower) {
            this.maxPower = maxPower;
        }

        public boolean isReceiver() {
            return receiver;
        }

        public void setReceiver(boolean receiver) {
            this.receiver = receiver;
        }

        public void disableTransfer() {
            transferDisabled = true;
        }

        public boolean isTransferDisabled() {
            return transferDisabled;
        }
    }
}
