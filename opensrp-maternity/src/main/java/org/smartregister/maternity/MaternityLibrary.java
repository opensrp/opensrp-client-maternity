package org.smartregister.maternity;

import org.smartregister.Context;
import org.smartregister.repository.Repository;
import org.smartregister.repository.UniqueIdRepository;
import org.smartregister.sync.ClientProcessorForJava;
import org.smartregister.sync.helper.ECSyncHelper;
import org.smartregister.view.activity.DrishtiApplication;

import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 2019-11-27
 */

public class MaternityLibrary {


    private static MaternityLibrary instance;
    private final Context context;
    private final Repository repository;
    private OpdConfiguration opdConfiguration;
    private ECSyncHelper syncHelper;

    private UniqueIdRepository uniqueIdRepository;

    private Compressor compressor;
    private int applicationVersion;
    private int databaseVersion;

    private Yaml yaml;

    private OpdRulesEngineHelper opdRulesEngineHelper;

    protected MaternityLibrary(@NonNull Context context, @NonNull OpdConfiguration opdConfiguration
            , @NonNull Repository repository, int applicationVersion, int databaseVersion) {
        this.context = context;
        this.opdConfiguration = opdConfiguration;
        this.repository = repository;
        this.applicationVersion = applicationVersion;
        this.databaseVersion = databaseVersion;

        // Initialize configs processor
        initializeYamlConfigs();
    }

    public static void init(Context context, @NonNull Repository repository, @NonNull OpdConfiguration opdConfiguration
            , int applicationVersion, int databaseVersion) {
        if (instance == null) {
            instance = new OpdLibrary(context, opdConfiguration, repository, applicationVersion, databaseVersion);
        }
    }

    public static OpdLibrary getInstance() {
        if (instance == null) {
            throw new IllegalStateException("Instance does not exist!!! Call "
                    + OpdLibrary.class.getName()
                    + ".init method in the onCreate method of "
                    + "your Application class");
        }
        return instance;
    }

    @NonNull
    public Context context() {
        return context;
    }

    @NonNull
    public UniqueIdRepository getUniqueIdRepository() {
        if (uniqueIdRepository == null) {
            uniqueIdRepository = new UniqueIdRepository(getRepository());
        }
        return uniqueIdRepository;
    }

    @NonNull
    public OpdCheckInRepository getCheckInRepository() {
        if (checkInRepository == null) {
            checkInRepository = new OpdCheckInRepository(getRepository());
        }

        return checkInRepository;
    }

    @NonNull
    public OpdVisitRepository getVisitRepository() {
        if (visitRepository == null) {
            visitRepository = new OpdVisitRepository(getRepository());
        }

        return visitRepository;
    }

    @NonNull
    public OpdDetailsRepository getOpdDetailsRepository() {
        if (opdDetailsRepository == null) {
            opdDetailsRepository = new OpdDetailsRepository(getRepository());
        }
        return opdDetailsRepository;
    }

    @NonNull
    public OpdDiagnosisAndTreatmentFormRepository getOpdDiagnosisAndTreatmentFormRepository() {
        if (opdDiagnosisAndTreatmentFormRepository == null) {
            opdDiagnosisAndTreatmentFormRepository = new OpdDiagnosisAndTreatmentFormRepository(getRepository());
        }
        return opdDiagnosisAndTreatmentFormRepository;
    }

    @NonNull
    public OpdDiagnosisRepository getOpdDiagnosisRepository() {
        if (opdDiagnosisRepository == null) {
            opdDiagnosisRepository = new OpdDiagnosisRepository(getRepository());
        }
        return opdDiagnosisRepository;
    }

    @NonNull
    public OpdTestConductedRepository getOpdTestConductedRepository() {
        if (opdTestConductedRepository == null) {
            opdTestConductedRepository = new OpdTestConductedRepository(getRepository());
        }
        return opdTestConductedRepository;
    }

    @NonNull
    public OpdTreatmentRepository getOpdTreatmentRepository() {
        if (opdTreatmentRepository == null) {
            opdTreatmentRepository = new OpdTreatmentRepository(getRepository());
        }
        return opdTreatmentRepository;
    }

    @NonNull
    public OpdServiceDetailRepository getOpdServiceDetailRepository() {
        if (opdServiceDetailRepository == null) {
            opdServiceDetailRepository = new OpdServiceDetailRepository(getRepository());
        }
        return opdServiceDetailRepository;
    }

    @NonNull
    public OpdVisitSummaryRepository getOpdVisitSummaryRepository() {
        if (opdVisitSummaryRepository == null) {
            opdVisitSummaryRepository = new OpdVisitSummaryRepository(getRepository());
        }
        return opdVisitSummaryRepository;
    }

    @NonNull
    public Repository getRepository() {
        return repository;
    }


    @NonNull
    public ECSyncHelper getEcSyncHelper() {
        if (syncHelper == null) {
            syncHelper = ECSyncHelper.getInstance(context().applicationContext());
        }
        return syncHelper;
    }

    @NonNull
    public OpdConfiguration getOpdConfiguration() {
        return opdConfiguration;
    }

    @NonNull
    public Compressor getCompressor() {
        if (compressor == null) {
            compressor = Compressor.getDefault(context().applicationContext());
        }

        return compressor;
    }

    @NonNull
    public ClientProcessorForJava getClientProcessorForJava() {
        return DrishtiApplication.getInstance().getClientProcessor();
    }


    public int getDatabaseVersion() {
        return databaseVersion;
    }

    public int getApplicationVersion() {
        return applicationVersion;
    }

    private void initializeYamlConfigs() {
        Constructor constructor = new Constructor(YamlConfig.class);
        TypeDescription customTypeDescription = new TypeDescription(YamlConfig.class);
        customTypeDescription.addPropertyParameters(YamlConfigItem.GENERIC_YAML_ITEMS, YamlConfigItem.class);
        constructor.addTypeDescription(customTypeDescription);
        yaml = new Yaml(constructor);
    }

    @NonNull
    public Iterable<Object> readYaml(@NonNull String filename) throws IOException {
        InputStreamReader inputStreamReader = new InputStreamReader(
                context.applicationContext().getAssets().open((FilePath.FOLDER.CONFIG_FOLDER_PATH + filename)));
        return yaml.loadAll(inputStreamReader);
    }

    @NonNull
    public OpdRulesEngineHelper getOpdRulesEngineHelper() {
        if (opdRulesEngineHelper == null) {
            opdRulesEngineHelper = new OpdRulesEngineHelper();
        }

        return opdRulesEngineHelper;
    }
}
