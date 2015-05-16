package com.liveramp.daemon_lib.executors;

import com.liveramp.daemon_lib.JobletConfig;
import com.liveramp.daemon_lib.JobletFactory;
import com.liveramp.daemon_lib.executors.processes.ProcessController;
import com.liveramp.daemon_lib.executors.processes.ProcessControllerException;
import com.liveramp.daemon_lib.utils.DaemonException;
import com.liveramp.daemon_lib.utils.ForkedJobletRunner;
import com.liveramp.daemon_lib.utils.JobletConfigMetadata;
import com.liveramp.daemon_lib.utils.JobletConfigStorage;

public class ForkedJobletExecutor<T extends JobletConfig> implements JobletExecutor<T> {
  private final JobletConfigStorage<T> configStorage;
  private final ProcessController<JobletConfigMetadata> processController;
  private final ForkedJobletRunner jobletRunner;
  private final int maxProcesses;
  private final Class<? extends JobletFactory<? extends T>> jobletFactoryClass;

  public ForkedJobletExecutor(int maxProcesses, Class<? extends JobletFactory<? extends T>> jobletFactoryClass, JobletConfigStorage<T> configStorage, ProcessController<JobletConfigMetadata> processController, ForkedJobletRunner jobletRunner) {
    this.maxProcesses = maxProcesses;
    this.jobletFactoryClass = jobletFactoryClass;
    this.configStorage = configStorage;
    this.processController = processController;
    this.jobletRunner = jobletRunner;
  }

  @Override
  public void execute(T config) throws DaemonException {
    try {
      String identifier = configStorage.storeConfig(config);
      int pid = jobletRunner.run(jobletFactoryClass, configStorage, identifier);
      processController.registerProcess(pid, new JobletConfigMetadata(identifier));
    } catch (Exception e) {
      throw new DaemonException(e); // TODO(asarkar): is this okay?
    }
  }

  @Override
  public boolean canExecuteAnother() {
    try {
      return processController.getProcesses().size() < maxProcesses;
    } catch (ProcessControllerException e) {
      // TODO(asarkar): consider retrying?
      throw new RuntimeException(e);
    }
  }
}
