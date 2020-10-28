NODE_LIST = 'mlpc-gpu-validation-tf'
if ('NODE_LIST' in params) {
    echo "NODE_LIST in params"
    if (params.NODE_LIST)
        echo "params.NODE_LIST is true"
    else
        echo "params.NODE_LIST is false"
    NODE_LIST=params.NODE_LIST
}
echo "NODE_LIST: $NODE_LIST"

// Functions
@NonCPS
String getCommitID(String txt) {
    def matches = (txt =~ /COMMIT_ID=(.*)/)
    if (matches) {
        match = matches[0]
        //[COMMIT_ID=431123ab22a69a1b46633a8c66d085a255275087, 431123ab22a69a1b46633a8c66d085a255275087]
        out = match[1]
        return out
    }
}

@NonCPS
String getCommitAuthor(String txt) {
    def matches = (txt =~ /COMMIT_AUTHOR=(.*)/)
    if (matches) {
        match = matches[0]
        out = match[1]
        return out
    }
}

@NonCPS
String getCommitDate(String txt) {
    def matches = (txt =~ /COMMIT_DATE=(.*)/)
    if (matches) {
        match = matches[0]
        out = match[1]
        return out
    }
}

@NonCPS
String getCommitSubject(String txt) {
    def matches = (txt =~ /COMMIT_SUBJECT=(.*)/)
    if (matches) {
        match = matches[0]
        out = match[1]
        return out
    }
}

@NonCPS
String getCommitEmail(String txt) {
    def matches = (txt =~ /COMMIT_OWNER_EMAIL=(.*)/)
    if (matches) {
        match = matches[0]
        out = match[1]
        return out
    }
}

@NonCPS
String getPrepareSourceResult(String txt) {
    def matches = (txt =~ /PREPARE_SOURCE_RESULT=(.*)/)
    if (matches) {
        match = matches[0]
        out = match[1]
        return out
    }
}

@NonCPS
String getBuildResult(String txt) {
    def matches = (txt =~ /RESULT=(.*)/)
    if (matches) {
        match = matches[0]
        out = match[1]
        return out
    }
}
String wrapHTML(String wrapper, String txt, String opt1) {
    if ('' == opt1) {
        wrapper1 = '<' + wrapper + '>'
    } else {
        wrapper1 = '<' + wrapper + ' ' + opt1 + '>'
    }
    wrapper2 = '</' + wrapper + '>'
    return wrapper1 + txt + wrapper2
}

String insText(String ins, String txt) {
    return txt.replace("TO_BE_REPLACED", ins)
}

String bindURL(String logname) {
    url = 'https://jenkins-aten-caffe2.sh.intel.com/job/' + JOB_NAME +
             '/' + BUILD_ID + '/artifact/' + logname
    return url
}

String bindLastURL(String logname) {
    build_id = sh(
        returnStdout: true,
        script: '''
        echo -n $(wget --no-check-certificate \
            -qO- https://jenkins-aten-caffe2.sh.intel.com/job/${JOB_NAME}/lastSuccessfulBuild/buildNumber)
        '''
    )
    url = 'https://jenkins-aten-caffe2.sh.intel.com/job/' + JOB_NAME +
             '/' + build_id + '/artifact/' + logname
    return url
}


DOWNLOAD_BASELINE = "Tensorflow_dGPU_DPCPP_Nightly"
if ('DOWNLOAD_BASELINE' in params) {
    echo "DOWNLOAD_BASELINE in params"
    if (params.DOWNLOAD_BASELINE != '') {
        DOWNLOAD_BASELINE = params.DOWNLOAD_BASELINE
        echo DOWNLOAD_BASELINE
    }
}
echo "DOWNLOAD_BASELINE: $DOWNLOAD_BASELINE"

COPY_NUM = 'lastSuccessfulBuild'
if ('COPY_NUM' in params) {
    echo "COPY_NUM in params"
    if (params.COPY_NUM != '') {
        COPY_NUM = params.COPY_NUM
        echo COPY_NUM
    }
}
echo "COPY_NUM: $COPY_NUM"

BRANCH = 'dev-v1.14.0-rc0'
if ('BRANCH' in params) {
    echo "BRANCH in params"
    if (params.BRANCH != '') {
        BRANCH = params.BRANCH
        echo BRANCH
    }
}
echo "COPY_NUM: $COPY_NUM"

if ('COMPILER_DIR' in params) {
    echo "COMPILER_DIR in params"
    if (params.COMPILER_DIR != '') {
        COMPILER_DIR = params.COMPILER_DIR
    }
}
echo "COMPILER_DIR: $COMPILER_DIR"

EMAIL = true
if ('EMAIL' in params) {
    if (params.EMAIL == 'YES') {
        EMAIL = true
    }
    if (params.EMAIL != 'YES') {
        EMAIL = false
    }
}
echo "EMAIL: $EMAIL"

MODEL_ENABLE = false
if ('MODEL' in params) {
    if (params.MODEL == 'YES') {
        MODEL_ENABLE = true
    }
    if (params.MODEL != 'YES') {
        MODEL_ENABLE = false
    }
}
echo "MODEL_ENABLE: $MODEL_ENABLE"


RESULT = "SUCCESS"
String COMMIT_OWNER_EMAIL = ""
BUILD_STAGE = ""
FAIL_NUM   = 0
FLAKY_NUM  = 0
TIMEOUT_NUM= 0
COMMIT_OWNER_EMAIL = ""
COMMIT_ID =""
COMMIT_OWNER_NAME=""
COMMIT_SUBJECT = ""

currentBuild.displayName = currentBuild.number.toString() + "#" + BRANCH

node(NODE_LIST) {
try{
    WORKSPACE1 = "${WORKSPACE}/nightly_ccpp"
    TARGET_TF = "${WORKSPACE1}/tensorflow"

    TYPE="CCPP"

    FAIL_LOG    = "${WORKSPACE1}/${TYPE}_FAIL_num.txt"
    FLAKY_LOG   = "${WORKSPACE1}/${TYPE}_FLAKY_num.txt"
    TIMEOUT_LOG = "${WORKSPACE1}/${TYPE}_TIMEOUT_num.txt"

    FAIL_CASES    = "${WORKSPACE1}/${TYPE}_FAIL_cases.txt"
    FLAKY_CASES   = "${WORKSPACE1}/${TYPE}_FLAKY_cases.txt"
    TIMEOUT_CASES = "${WORKSPACE1}/${TYPE}_TIMEOUT_cases.txt"
    RESULT_LOG  = "${WORKSPACE1}/${TYPE}_RESULT.txt"

    FAIL_DIFF    = "${WORKSPACE1}/${TYPE}_FAIL_diff.txt"
    FLAKY_DIFF   = "${WORKSPACE1}/${TYPE}_FLAKY_diff.txt"
    TIMEOUT_DIFF = "${WORKSPACE1}/${TYPE}_TIMEOUT_diff.txt"

    FAIL_REFER    = "${WORKSPACE1}/refer/${TYPE}_FAIL_cases.txt"
    FLAKY_REFER   = "${WORKSPACE1}/refer/${TYPE}_FLAKY_cases.txt"
    TIMEOUT_REFER = "${WORKSPACE1}/refer/${TYPE}_TIMEOUT_cases.txt"

    FAIL_REFER_NUM    = "${WORKSPACE1}/refer/${TYPE}_FAIL_num.txt"
    FLAKY_REFER_NUM   = "${WORKSPACE1}/refer/${TYPE}_FLAKY_num.txt"
    TIMEOUT_REFER_NUM = "${WORKSPACE1}/refer/${TYPE}_TIMEOUT_num.txt"

    deleteDir()



    // DPCPP
    WORKSPACE2 = "${WORKSPACE}/nightly_dpcpp"
    TARGET_TF = "${WORKSPACE2}/tensorflow"

    TYPE="DPCPP"

    UT_TOTAL        = "${WORKSPACE2}/${TYPE}_UT_TOTAL.txt"
    REFER_UT_TOTAL  = "${WORKSPACE}/refer/${TYPE}_UT_TOTAL.txt"
    FAIL_LOG2    = "${WORKSPACE2}/${TYPE}_FAIL_num.txt"
    FLAKY_LOG2   = "${WORKSPACE2}/${TYPE}_FLAKY_num.txt"
    TIMEOUT_LOG2 = "${WORKSPACE2}/${TYPE}_TIMEOUT_num.txt"

    FAIL_CASES2    = "${WORKSPACE2}/${TYPE}_FAIL_cases.txt"
    FLAKY_CASES2   = "${WORKSPACE2}/${TYPE}_FLAKY_cases.txt"
    TIMEOUT_CASES2 = "${WORKSPACE2}/${TYPE}_TIMEOUT_cases.txt"
    RESULT_LOG2  = "${WORKSPACE2}/${TYPE}_RESULT.txt"

    FAIL_DIFF2    = "${WORKSPACE2}/${TYPE}_FAIL_diff.txt"
    FLAKY_DIFF2   = "${WORKSPACE2}/${TYPE}_FLAKY_diff.txt"
    TIMEOUT_DIFF2 = "${WORKSPACE2}/${TYPE}_TIMEOUT_diff.txt"

    FAIL_REFER2    = "${WORKSPACE}/refer/${TYPE}_FAIL_cases.txt"
    FLAKY_REFER2   = "${WORKSPACE}/refer/${TYPE}_FLAKY_cases.txt"
    TIMEOUT_REFER2 = "${WORKSPACE}/refer/${TYPE}_TIMEOUT_cases.txt"

    FAIL_REFER_NUM2    = "${WORKSPACE}/refer/${TYPE}_FAIL_num.txt"
    FLAKY_REFER_NUM2   = "${WORKSPACE}/refer/${TYPE}_FLAKY_num.txt"
    TIMEOUT_REFER_NUM2 = "${WORKSPACE}/refer/${TYPE}_TIMEOUT_num.txt"

    stage("Clone Tensorflow DPCPP")
    {
        // DEBUG!
            // checkout([$class                           : 'GitSCM',
            //         branches                         : [[name: BRANCH]],
            //         //branches                         : [[name: "ats-perf-preview-ww33"]],
            //         //branches                         : [[name: "dev-v1.14.0-rc0"]],
            //         browser                          : [$class: 'AssemblaWeb', repoUrl: ''],
            //         doGenerateSubmoduleConfigurations: false,
            //         extensions                       : [[$class: 'RelativeTargetDirectory',
            //                                             relativeTargetDir: "${WORKSPACE2}/tensorflow"]],
            //         submoduleCfg                     : [],
            //         userRemoteConfigs                : [[credentialsId: "383917ea-b9f7-4c85-ad66-bc3f57f98b6c",
            //                                             url: "https://gitlab.devtools.intel.com/tf/tensorflow.git"]]])
        withEnv(["WORKSPACE2=${WORKSPACE2}"]) {
            sh '''
                mkdir -p ${WORKSPACE2}
                cp -r /home/tensorflow/workspace/source/tensorflow_model ${WORKSPACE2}/tensorflow
            '''
        }
    }
      copyArtifacts(
                projectName: DOWNLOAD_BASELINE,
                selector: specific(COPY_NUM),
                filter: "*",
                fingerprintArtifacts: true,
                target: "refer/")
      stage("Run UT on DPCPP")
      {

        withEnv(["WORKSPACE2=${WORKSPACE2}","BRANCH=${BRANCH}"]) {
          sh_out = sh(
                    returnStdout: true,
                    script: '''#!/bin/bash -x
                    set +x
                    cd ${WORKSPACE2}/tensorflow
                    _git_info=$(git log -1 --format='%H;%an;%ad;%s')
                    COMMIT_ID=$(echo $_git_info | cut -d';' -f1)
                    COMMIT_AUTHOR=$(echo $_git_info | cut -d';' -f2)
                    COMMIT_DATE=$(echo $_git_info | cut -d';' -f3)
                    COMMIT_SUBJECT=$(echo ${_git_info} | cut -d';' -f 4-)
                    echo COMMIT_ID=$COMMIT_ID
                    echo COMMIT_AUTHOR=$COMMIT_AUTHOR
                    echo COMMIT_OWNER_EMAIL=$(git log -1 --format='%ae')
                    echo COMMIT_DATE=$COMMIT_DATE
                    echo COMMIT_SUBJECT=$COMMIT_SUBJECT
                    echo PREPARE_SOURCE_RESULT=$clone_res
                    '''
                )
          B_I_KEYS = [
                        'BUILD_TIME',
                        'REPO_TYPE',
                        'SOURCE_DIR',
                        'CONFIG_PROJECT',
                        'PROJECT',
                        'BRANCH',
                        'COMMIT_ID',
                        'COMMIT_OWNER_NAME',
                        'COMMIT_OWNER_EMAIL',
                        'COMMIT_SUBMIT_DATE',
                        'COMMIT_SUBJECT',
                        'PREPARE_SOURCE_RESULT',
                        'PREPARE_SOURCE_FAIL_REASON'
                        ]

          COMMIT_OWNER_EMAIL = getCommitEmail(sh_out)
          echo "-------------COMMIT_INFO-----------------"
          COMMIT_OWNER_EMAIL = getCommitEmail(sh_out)
          println(COMMIT_OWNER_EMAIL)
          COMMIT_OWNER_NAME = getCommitAuthor(sh_out)
          println(COMMIT_OWNER_NAME)
          COMMIT_ID = getCommitID(sh_out)
          println(COMMIT_ID)
          COMMIT_SUBJECT = getCommitSubject(sh_out)
          println(COMMIT_SUBJECT)
        }

        withEnv(["FAIL_LOG2=${FAIL_LOG2}", "FLAKY_LOG2=${FLAKY_LOG2}", "TIMEOUT_LOG2=${TIMEOUT_LOG2}", "RESULT_LOG2=${RESULT_LOG2}",
              "FAIL_CASES2=${FAIL_CASES2}", "FLAKY_CASES2=${FLAKY_CASES2}", "TIMEOUT_CASES2=${TIMEOUT_CASES2}",
              "FAIL_DIFF2=${FAIL_DIFF2}", "FLAKY_DIFF2=${FLAKY_DIFF2}", "TIMEOUT_DIFF2=${TIMEOUT_DIFF2}",
              "FAIL_REFER2=${FAIL_REFER2}", "FLAKY_REFER2=${FLAKY_REFER2}", "TIMEOUT_REFER2=${TIMEOUT_REFER2}",
              "WORKSPACE2=${WORKSPACE2}", "UT_TOTAL=${UT_TOTAL}", "COMPILER_DIR=${COMPILER_DIR}"]) {
          dir("${WORKSPACE2}")
          {
            sh '''
            set -ex
            pwd
            cd ${WORKSPACE2}/tensorflow
            echo "${FAIL_LOG2} ${FLAKY_LOG2} ${TIMEOUT_LOG2}"
            # conda env
            source ~/.bashrc
            if [ -e /home/tensorflow/anaconda3/envs/tfgpu_nightly ];then
                source ~/anaconda3/bin/activate tfgpu_nightly
                echo -e "[ INFO ] Success Used Conda"
                echo -e "[ INFO ] Current Conda Venv: $(echo $PS1| awk -F ')' '{print $1}' ))"
            else
                echo -e "[ ERROR ] Can not use conda"
                exit 1
            fi

            # replace configure
            cp /home/tensorflow/workspace/conf/dpcpp/.tf_configure.bazelrc_nightly_ut ./.tf_configure.bazelrc

            unitTestLog="py_kernel_dpcpp.log"

            failLog="dpcpp_failed_cases.log"

            flakyLog="dpcpp_flaky_cases.log"

            timeoutLog="dpcpp_timeout_cases.log"

            #export COMPILER_DIR=/home/tensorflow/tools
            #export LD_LIBRARY_PATH=${COMPILER_DIR}/dpcpp_compiler/lib
            # beta09 compiler
            #export COMPILER_DIR=/home/tensorflow/intel/oneapi/compiler/latest/linux
            # temp compiler 20200930_000000
            #export COMPILER_DIR=/home/tensorflow/tools/Compiler_4rebase/20200930_000000/build/linux_prod/compiler/linux

            # default is oneapi
            if [ "" == "${COMPILER_DIR}" ]; then
                COMPILER_DIR=/home/tensorflow/intel/oneapi/compiler/latest/linux
            fi
            export LD_LIBRARY_PATH=${COMPILER_DIR}/lib:${COMPILER_DIR}/compiler/lib/intel64_lin
            echo LD_LIBRARY_PATH: $LD_LIBRARY_PATH

sed "s/build --action_env DPCPP_TOOLKIT_PATH=.*/build --action_env DPCPP_TOOLKIT_PATH=\\\"${COMPILER_DIR//\\//\\\\/}\\\"/" \
  .tf_configure.bazelrc \
  > .tf_configure.bazelrc.tmp
cp .tf_configure.bazelrc.tmp .tf_configure.bazelrc
            echo "------------> Config <-------------"
            cat .tf_configure.bazelrc
            echo "-----------------------------------"
            python --version
            echo "-----------------------------------"
            bazel clean --expunge --async
            which python
            # CMD
# DPCPP_COMPILER
# DEBUG!
            # origin nightly command
            #                               /home/tensorflow/bin/bazel test --verbose_failures --test_verbose_timeout_warnings \
                                            #--host_javabase=@local_jdk//:jdk \
                                            #--test_timeout 300,800,1200,3600 \
                                            #--local_test_jobs=6 \
                                            #-c opt \
                                            #--config=dpcpp \
                                            #--action_env=SYCL_ENABLE_HOST_DEVICE=1 \
                                            #--copt=-DDPCPP_ENABLE_DOUBLE \
                                            #-- //tensorflow/python/kernel_tests/... \
                                            #2>&1 | tee ${unitTestLog}
            # compiler 20200930_000000 + nightly command
            #/home/tensorflow/bin/bazel test --verbose_failures --test_verbose_timeout_warnings \
            #                                --distinct_host_configuration=false \
            #                                --host_javabase=@local_jdk//:jdk \
            #                                --test_timeout 300,450,1200,3600 \
            #                                --local_test_jobs=6 \
            #                                -c opt \
            #                                --config=dpcpp \
            #                                --action_env=SYCL_ENABLE_HOST_DEVICE=1 \
            #                                --copt=-DDPCPP_ENABLE_DOUBLE \
            #                                -- //tensorflow/python/kernel_tests/... \
            #                                2>&1 | tee ${unitTestLog}
            touch ${unitTestLog}
            echo "ERRORs: $(grep 'ERROR: ' ${unitTestLog})"
            if [ "$(grep 'ERROR: ' ${unitTestLog} | wc -l)" = "0" ] ; then
                RESULT="SUCCESS"
                echo ${RESULT}
            else
                RESULT="FAILURE"
                ERROR="1"
                echo "${RESULT} "
                exit 1
            fi
            grep "^FAILED: " py_kernel_dpcpp.log | sed "s/(.*)//g" > ${failLog} #dpcpp_failed_cases.log
            grep "^TIMEOUT: " py_kernel_dpcpp.log | sed "s/(.*)//g" >  ${timeoutLog}  #dpcpp_timeout_cases.log
            grep "^FLAKY: " py_kernel_dpcpp.log | sed "s/(.*)//g" > ${flakyLog}  #dpcpp_flaky_cases.log
            total=$(cat ${unitTestLog} | grep Executed | grep -oP 'out of \\d+' | cut -d' ' -f3)
            fail_n=$(sort dpcpp_failed_cases.log | uniq  | awk '{print NR}' | tail -n1)
            echo ${total} > ${UT_TOTAL}
            set -x
            echo "----------------------------------"
            if [ -s ${failLog} ]; then
                sort ${failLog} | uniq  | awk '{print NR}' | tail -n1 > ${FAIL_LOG2}
                sort ${failLog} | uniq  > ${FAIL_CASES2}
                sed -i 's#^#<li>#g' ${FAIL_CASES2}
                sed -i 's#$#</li>#g' ${FAIL_CASES2}
                if [ -f refer/DPCPP_FAIL_cases.txt ];then
                    comm -23 ${FAIL_CASES2} ${FAIL_REFER2} > ${FAIL_DIFF2} || true
                else
                    cat ${FAIL_CASES2} > ${FAIL_DIFF2}
                fi
                echo "------------- DIFF --------------"
                cat ${FAIL_DIFF2}  || true
            else
                echo "${failLog} empty"
                echo "0" > ${FAIL_LOG2}
                touch  ${FAIL_CASES2}
                touch  ${FAIL_DIFF2}
            fi
            echo "----------------------------------"
            if [ -s  ${timeoutLog} ]; then
                sort ${timeoutLog}  | uniq  | awk '{print NR}' | tail -n1 > ${TIMEOUT_LOG2}
                sort ${timeoutLog} | uniq  > ${TIMEOUT_CASES2}
                sed -i 's#^#<li>#g' ${TIMEOUT_CASES2}
                sed -i 's#$#</li>#g' ${TIMEOUT_CASES2}
                if [ -f refer/DPCPP_TIMEOUT_cases.txt  ];then
                    comm -23 ${TIMEOUT_CASES2} ${TIMEOUT_REFER2} > ${TIMEOUT_DIFF2} || true
                else
                    cat ${TIMEOUT_CASES2} > ${TIMEOUT_DIFF2}
                fi
                echo "------------- DIFF --------------"
                cat ${TIMEOUT_DIFF2}  || true
            else
                echo " ${timeoutLog} empty"
                echo "0" > ${TIMEOUT_LOG2}
                touch ${TIMEOUT_CASES2}
                touch ${TIMEOUT_DIFF2}
            fi
            echo "----------------------------------"
            if [ -s ${flakyLog} ]; then
                sort ${flakyLog} | uniq  | awk '{print NR}' | tail -n1 > ${FLAKY_LOG2}
                sort ${flakyLog} | uniq  > ${FLAKY_CASES2}
                sed -i 's#^#<li>#g' ${FLAKY_CASES2}
                sed -i 's#$#</li>#g' ${FLAKY_CASES2}
                if [ -f refer/DPCPP_FLAKY_cases.txt  ];then
                    comm -23 ${FLAKY_CASES2} ${FLAKY_REFER2} > ${FLAKY_DIFF2} || true
                else
                    cat ${FLAKY_CASES2}  > ${FLAKY_DIFF2}
                fi
                echo "------------- DIFF --------------"
                cat ${FLAKY_DIFF2}  || true
            else
                echo "${flakyLog} empty"
                echo "0" > ${FLAKY_LOG2}
                touch ${FLAKY_CASES2}
                touch ${FLAKY_DIFF2}
            fi
            [ -e $FAIL_LOG2      ] && cp $FAIL_LOG2      ${WORKSPACE}/
            [ -e $FLAKY_LOG2     ] && cp $FLAKY_LOG2     ${WORKSPACE}/
            [ -e $TIMEOUT_LOG2   ] && cp $TIMEOUT_LOG2   ${WORKSPACE}/
            [ -e $FAIL_CASES2    ] && cp $FAIL_CASES2    ${WORKSPACE}/
            [ -e $FLAKY_CASES2   ] && cp $FLAKY_CASES2   ${WORKSPACE}/
            [ -e $TIMEOUT_CASES2 ] && cp $TIMEOUT_CASES2 ${WORKSPACE}/
            [ -e $RESULT_LOG2    ] && cp $RESULT_LOG2    ${WORKSPACE}/
            [ -e $FAIL_DIFF2     ] && cp $FAIL_DIFF2     ${WORKSPACE}/
            [ -e $FLAKY_DIFF2    ] && cp $FLAKY_DIFF2    ${WORKSPACE}/
            [ -e $TIMEOUT_DIFF2  ] && cp $TIMEOUT_DIFF2  ${WORKSPACE}/
            [ -e $UT_TOTAL       ] && cp $UT_TOTAL       ${WORKSPACE}/
            '''
          }
        } // DPCPP

      }

      archiveArtifacts artifacts: '*_cases.txt, *_num.txt, *UT_TOTAL.txt', excludes: null
      TOTAL_UT_NUM = readFile UT_TOTAL
      FAIL_NUM = readFile FAIL_LOG2
      FLAKY_NUM = readFile FLAKY_LOG2
      TIMEOUT_NUM = readFile TIMEOUT_LOG2
      FAIL_CASES = readFile FAIL_CASES2
      FLAKY_CASES = readFile FLAKY_CASES2
      TIMEOUT_CASES = readFile TIMEOUT_CASES2

      //FAIL_DIFF = readFile FAIL_DIFF2
      //FLAKY_DIFF = readFile FLAKY_DIFF2
      //TIMEOUT_DIFF = readFile TIMEOUT_DIFF2

      REFER_TOTAL_UT_NUM = readFile REFER_UT_TOTAL
      FAIL_REFER = readFile FAIL_REFER2
      FLAKY_REFER = readFile FLAKY_REFER2
      TIMEOUT_REFER = readFile TIMEOUT_REFER2

      FAIL_REFER_NUM   = readFile FAIL_REFER_NUM2
      FLAKY_REFER_NUM  = readFile FLAKY_REFER_NUM2
      TIMEOUT_REFER_NUM  = readFile TIMEOUT_REFER_NUM2

    stage("Build WHL") {
        withEnv(["WORKSPACE_=${WORKSPACE2}", "COMPILER_DIR=${COMPILER_DIR}"]) {
            dir("${WORKSPACE2}") {
                sh '''
# Copy source code for model test build. 
# UT and model use 2 different folder, and that is to avoid deleting bazel log.
rm -rf tensorflow_model
cp -r tensorflow ./tensorflow_model

echo "Build whl file"
cd tensorflow_model
if [ "" == "${COMPILER_DIR}" ]; then
    COMPILER_DIR=/home/tensorflow/intel/oneapi/compiler/latest/linux
fi
export LD_LIBRARY_PATH=${COMPILER_DIR}/lib:${COMPILER_DIR}/compiler/lib/intel64_lin
echo LD_LIBRARY_PATH: $LD_LIBRARY_PATH
TEST_HOMEBASE=/home/songhu1x
source ${TEST_HOMEBASE}/anaconda3/bin/activate py36gpu_dpcpp
#  DEBUG!
#pip uninstall -y tensorflow
#bazel clean --expunge --async
#bazel build --verbose_failures --distinct_host_configuration=false \
#            --action_env=SYCL_ENABLE_HOST_DEVICE=1 -c opt \
#            --copt=-DDPCPP_ENABLE_DOUBLE --config=dpcpp \
#            //tensorflow/tools/pip_package:build_pip_package >& bazel_build.log
#bazel-bin/tensorflow/tools/pip_package/build_pip_package ${WORKSPACE_}/whl

cd ${WORKSPACE_}
TEST_HOMEBASE=/home/songhu1x
source ${TEST_HOMEBASE}/anaconda3/bin/activate py36gpu_dpcpp
#  DEBUG!
pip uninstall -y tensorflow
#  DEBUG!
#pip install ${WORKSPACE_}/whl/*.whl
pip install /home/tensorflow/workspace/whl/nightly_whl/tensorflow-1.14.0rc0-cp36-cp36m-linux_x86_64.whl
echo $?
source ${TEST_HOMEBASE}/anaconda3/bin/activate dpcpp_train
#  DEBUG!
pip uninstall -y tensorflow
#  DEBUG!
#pip install ${WORKSPACE_}/whl/*.whl
pip install /home/tensorflow/workspace/whl/nightly_whl/tensorflow-1.14.0rc0-cp36-cp36m-linux_x86_64.whl
echo $?
                '''
            }
        }
    }
    if (MODEL_ENABLE) {
    stage("Inference") {
        ///////// INFERENCE ///////// 
        // test_model_gen9_for_nightly node: mlpc-gpu-validation-tf
        // 'bert_base resnet50_v1 transformer_lt ssd'
        //  DEBUG!
        jks_job_gen9 = 'test_model_gen9_for_nightly'
        onebuild = build job: jks_job_gen9, propagate: false, parameters: [
            [$class: 'StringParameterValue', name: 'TEST_NODE', value: 'mlpc-gpu-validation-tf_model_only' ],
            [$class: 'StringParameterValue', name: 'TEST_HOMEBASE', value: '/home/songhu1x' ],
            [$class: 'StringParameterValue', name: 'MODEL_TYPE_STR', 
                value: 'resnet50_v1 bert_base ssd' ],
            [$class: 'StringParameterValue', name: 'ACC_TYPE', value: 'ALL' ],
            [$class: 'StringParameterValue', name: 'CMT', value: "123" ],
        ]
        task_status = onebuild.result
        task_number = onebuild.number
        task_dur = onebuild.duration.intdiv(1000)
        print(jks_job_gen9 + " Duration: " + task_dur)
        COPY_NUM = task_number.toString()
        copyArtifacts(
            projectName: jks_job_gen9,
            selector: specific(COPY_NUM),
            filter: "*.log,result.txt",
            excludes: 'tensorflow', // ignore source code dir
            fingerprintArtifacts: true,
            target: WORKSPACE )
        archiveArtifacts artifacts: '*.log,result.txt', excludes: null
    }
    }
    if ( EMAIL ) {
      stage("Email") {
            println " ------> Email Report <------- "
            echo "--------RESULT----------"
            println(RESULT)
            COLOR = "#00FF00"
            EMOJI = "✌"
            if(RESULT != 'SUCCESS')
            {
                COLOR = "#FF0000"
                EMOJI = "✋"
            }
            if (MODEL_ENABLE) {
            sh '''
            # Summarize result of inference
            # need ~/.ssh/config is configured 
            #  DEBUG!
            #cp -r /home/tensorflow/workspace/source/tools ${WORKSPACE}/
            git clone -b debug_new_model \
                songhu1x-gitlab:songhu1x/tensorflow_gen9_dg1_test.git \
                ${WORKSPACE}/tools
            '''
            // Model Performance Test
            def models = [
                "resnet50_v1", "bert_base", "ssd", "transformer_lt"
            ]
            def model_name = [
                "ResNet50", "BERT_BASE", "SSD", "Transformer_LT"
            ]
            def model_name_map = [:]
            model_name_map["resnet50_v1"] = 'ResNet50'
            model_name_map["bert_base"] = 'BERT_BASE'
            model_name_map["ssd"] = 'SSD'
            model_name_map["transformer_lt"] = 'Transformer_LT'
            def type = [ "inference", "training" ] //
            def precision = [ 'fp32', 'fp16', 'bfp16', 'int8' ] //
        def perf_tr = ''
        // skip performance
        def skip_list = [
            ['inference','bert_base','int8'],
            ['inference','ssd','bfp16'],
            ['inference','transformer_lt','fp32'],
            ['inference','transformer_lt','fp16'],
            ['inference','transformer_lt','bfp16'],
            ['inference','transformer_lt','int8'],
            ['training','resnet50_v1','fp16'],
            ['training','resnet50_v1','int8'],
            ['training','bert_base','fp16'],
            ['training','bert_base','int8'],
            ['training','ssd','fp32'],
            ['training','ssd','fp16'],
            ['training','ssd','bfp16'],
            ['training','ssd','int8'],
            ['training','transformer_lt','fp32'],
            ['training','transformer_lt','fp16'],
            ['training','transformer_lt','bfp16'],
            ['training','transformer_lt','int8'],
        ]
        for ( t in type ) {
            for ( i = 0; i < models.size(); i++ ) {
                m = models[i]
                n = model_name[i]
                for ( a in precision ) {
                    flg = false
                    for ( sk in skip_list ) {
                        if ( sk[0] == t && sk[1] == m && sk[2] == a ) {
                            flg = true
                            break
                        }
                    }
                    if (flg == true) {
                        continue
                    }
                    logname_ = "performance_" + t + "_" + m + "_" + a + "_GEN9"
                    println(logname_)
                    withEnv(["logname_=${logname_}","t=${t}","m=${m}","a=${a}"]) {
                        logname = sh(
                            returnStdout: true,
                            script: '''set -x
                            find . -maxdepth 1 -name "${logname_}*" | cut -d/ -f2
                            ''')
                        logname_ref = sh(
                            returnStdout: true,
                            script: '''set -x
                            pushd refer >& /dev/null
                            find . -maxdepth 1 -name "${logname_}*" | cut -d/ -f2
                            popd >& /dev/null
                            ''')
                        throughput = sh(
                            returnStdout: true,
                            script: '''set -x
                            cat ${logname_}* \
                                | grep Throughput \
                                | tail -1 \
                                | sed 's/ \\+/ /g' | cut -d' ' -f2 || true
                            '''
                        )
                        throughput_ref = sh(
                            returnStdout: true,
                            script: '''set -x
                            pushd refer >& /dev/null
                            cat ${logname_}* | grep Throughput | tail -1 | sed 's/ \\+/ /g' | cut -d' ' -f2 || true
                            popd >& /dev/null
                            '''
                        )
                        latency = sh(
                            returnStdout: true,
                            script: '''set -x
                            cat ${logname_}* | grep Latency | tail -1 | sed 's/ \\+/ /g' | cut -d' ' -f2  || true
                            '''
                        )
                        latency_ref = sh(
                            returnStdout: true,
                            script: '''set -x
                            pushd refer >& /dev/null
                            cat ${logname_}* | grep Latency | tail -1 | sed 's/ \\+/ /g' | cut -d' ' -f2 || true
                            popd >& /dev/null
                            '''
                        )
                        // calc 
                        println('throughput:     ' + throughput)
                        println('latency:        ' + latency)
                        println('throughput_ref: ' + throughput_ref)
                        println('latency_ref:    ' + latency_ref)
                        if (
                            throughput && latency && throughput_ref && latency_ref
                        ) {
                            throughput_ret = (throughput.toFloat() - throughput_ref.toFloat()) * 100 / throughput_ref.toFloat()
                            latency_ret = (latency.toFloat() - latency_ref.toFloat()) * 100 / latency_ref.toFloat()
                        }
                        try {
                            println(throughput_ret)
                            throughput_retstr = sprintf("%.2f", throughput_ret)
                        } catch (e) {
                            throughput_ret = ''
                        }
                        try {
                            println(latency_ret)
                            latency_retstr = sprintf("%.2f", latency_ret)
                        } catch (e) {
                            latency_retstr = ''
                        }
                    } // withEnv
                    // println('1: ' + logname_)
                    // Performance Part
                    td_option = 'style="border:2px solid #000000;text-align: center;"'
                    perf_str = wrapHTML('tr', 
                                    wrapHTML('td', n, td_option)
                                    + wrapHTML('td', t, td_option)
                                    + wrapHTML('td', a, td_option)
                                    //+ wrapHTML('td', '', td_option)   // bs
                                    + wrapHTML('td', throughput, td_option)   // throughput
                                    + wrapHTML('td', throughput_retstr.toString() + '%', td_option)   // throughput percent
                                    + wrapHTML('td', latency, td_option)   // latency
                                    + wrapHTML('td', latency_retstr.toString() + '%', td_option)   // latency percent
                                    + wrapHTML(
                                        'td', 
                                        wrapHTML(
                                            'a', 
                                            'log',
                                            'href='+bindLastURL(logname_ref)),
                                        td_option)
                                    + wrapHTML(
                                        'td', 
                                        wrapHTML(
                                            'a', 
                                            'log',
                                            'href='+bindURL(logname)),
                                        td_option),
                                    ''
                                )
                    perf_tr += perf_str
                } // for a
            } // for model
        } // for type

        // ACC Part
        def acc_tr = ''
        def acc_white_list = [
            ['accuracy','resnet50_v1','fp32'],
            ['accuracy','bert_base','fp32'],
            //['accuracy','transformer_lt','bfp16'],
        ]
        for (acc in acc_white_list) {
            t = acc[0]
            m = acc[1]
            a = acc[2]
            n = model_name_map[m]
            logname_ = t + "_" + m + "_" + a + "_GEN9"
            withEnv(["logname_=${logname_}","t=${t}","m=${m}","a=${a}"]) {
                logname = sh(
                    returnStdout: true,
                    script: '''set -x
                    find . -maxdepth 1 -name "${logname_}*" | cut -d/ -f2
                    ''')
                    println(logname)
                logname_ref = sh(
                    returnStdout: true,
                    script: '''set -x
                    pushd refer >& /dev/null
                    find . -maxdepth 1 -name "${logname_}*" | cut -d/ -f2
                    popd >& /dev/null
                    ''')
            }
            println("logname:     " + logname)
            println("logname_ref: " + logname_ref)
            withEnv(["logname_=${logname}","logname_ref=${logname_ref}","t=${t}","m=${m}","a=${a}"]) {
                    accuracy = sh(
                        returnStdout: true,
                        script: '''set -x
                        source ${WORKSPACE}/tools/tf_inference_chk_function.sh
                        VERBOSE=1 accuracy_${m}_${a}_chk ${logname_} | grep "accuracy:" | cut -d':' -f2
                        '''
                    )
                    accuracy_ref = sh(
                        returnStdout: true,
                        script: '''set -x
                        source ${WORKSPACE}/tools/tf_inference_chk_function.sh
                        pushd refer >& /dev/null
                        VERBOSE=1 accuracy_${m}_${a}_chk ${logname_ref} \
                            | grep "accuracy:" \
                            | cut -d':' -f2
                        popd >& /dev/null
                        '''
                    )
            } // withEnv
            println('accuracy:     ' + accuracy)
            println('accuracy_ref: ' + accuracy_ref)
            if (
                accuracy && accuracy_ref && accuracy != '' && accuracy_ref != ''
            ) {
                accuracy_ret = (accuracy.toFloat() - accuracy_ref.toFloat()) * 100 / accuracy_ref.toFloat()
            }
            try {
                accuracy_retstr = sprintf("%.2f", accuracy_ret)
            } catch (e) {
                accuracy_retstr = ''
            }
            td_option = 'style="border:2px solid #000000;text-align: center;"'
            acc_str = wrapHTML('tr', 
                        wrapHTML('td', n, td_option)
                        + wrapHTML('td', 'accuracy', td_option)
                        + wrapHTML('td', a, td_option)
                        //+ wrapHTML('td', '', td_option)   // bs
                        + wrapHTML('td', accuracy, td_option)   // accuracy
                        + wrapHTML('td', accuracy_retstr.toString() + '%', td_option)   // accuracy percent
                        + wrapHTML(
                            'td', 
                            wrapHTML(
                                'a', 
                                'log',
                                'href='+bindLastURL(logname_ref)),
                            td_option)
                        + wrapHTML(
                            'td', 
                            wrapHTML(
                                'a', 
                                'log',
                                'href='+bindURL(logname)),
                            td_option),
                        ''
            )
            acc_tr += acc_str
        } // for ACC





            def perf_table_html = """
<h2>Model Results</h2>
<h3>Performance Test</h3>
<div class="performace">
    <table name="perf_table" style="border:2px solid #000000;border-collapse: collapse;">
        <thead>
<th width="20% " style="border:2px solid #000000;text-align: center;background-color: #d3d3d3; ">Model Case</th>
<th width="10% " style="border:2px solid #000000;text-align: center;background-color: #d3d3d3; ">Type</th>
<th width="10% " style="border:2px solid #000000;text-align: center;background-color: #d3d3d3; ">Precision</th>
<th width="20% " style="border:2px solid #000000;text-align: center;background-color: #d3d3d3; ">Throughput</th>
<th width="20% " style="border:2px solid #000000;text-align: center;background-color: #d3d3d3; ">Throughput Status</th>
<th width="20% " style="border:2px solid #000000;text-align: center;background-color: #d3d3d3; ">Latency</th>
<th width="20% " style="border:2px solid #000000;text-align: center;background-color: #d3d3d3; ">Latency Status</th>
<th width="5% " style="border:2px solid #000000;text-align: center;background-color: #d3d3d3; ">Ref Log</th>
<th width="5% " style="border:2px solid #000000;text-align: center;background-color: #d3d3d3; ">Log</th>
    </thead>
    <tbody><tr>TO_BE_REPLACED</tr></tbody>
    </table>
</div>
            """
            perf_table_html = insText(perf_tr, perf_table_html)

            // Accuracy Test
            def accuracy_table_html = """
<h3>Accuracy Test</h3>
<div class="accuracy">
    <table name="acc_table" style="border:2px solid #000000;border-collapse: collapse;">
        <thead>
<th width="20% " style="border:2px solid #000000;text-align: center;background-color: #d3d3d3; ">Model Case</th>
<th width="10% " style="border:2px solid #000000;text-align: center;background-color: #d3d3d3; ">Type</th>
<th width="10% " style="border:2px solid #000000;text-align: center;background-color: #d3d3d3; ">Precision</th>
<th width="20% " style="border:2px solid #000000;text-align: center;background-color: #d3d3d3; ">Accuracy</th>
<th width="20% " style="border:2px solid #000000;text-align: center;background-color: #d3d3d3; ">Accuracy Status</th>
<th width="5% " style="border:2px solid #000000;text-align: center;background-color: #d3d3d3; ">Ref Log</th>
<th width="5% " style="border:2px solid #000000;text-align: center;background-color: #d3d3d3; ">Log</th>
    </thead>
    <tbody><tr>TO_BE_REPLACED</tr></tbody>
    </table>
</div>
            """
            accuracy_table_html = insText(acc_tr, accuracy_table_html)
            } else { // MODEL_ENABLE
                perf_table_html = ''
                accuracy_table_html = ''
            } // MODEL_ENABLE
            // DEBUG!
            emailext(
                mimeType: 'text/html',
                from: 'Tensorflow_Test@intel.com',
                //to: 'daisy.deng@intel.com;maozhou.ge@intel.com;guizi.li@intel.com;shufan.wu@intel.com;wei2.zhu@intel.com;ximeng.cui@intel.com;cc:chuandongx.xu@intel.com;chaofan.li@intel.com;cc:baochenx.yang@intel.com;karen.xiang@intel.com;cherry.zhang@intel.com;leicong.li@intel.com;yiqiang.li@intel.com;zhoulong.jiang@intel.com;quintin.wang@intel.com;shengzhi.chen@intel.com;cc:huimingx.song@intel.com' ,
                to: 'huimingx.song@intel.com' ,
                subject: '(' + RESULT + ')' + ' Tensorflow dGPU Unit Tests On DPCPP Nightly Test Report',
                body:"""
                <html>
                    <head>
                        <link href="https://fonts.googleapis.com/css2?family=Open+Sans:ital,wght@0,300;0,400;0,600;0,700;0,800;1,300;1,400;1,600;1,700;1,800&display=swap" rel="stylesheet">
                        <style type="text/css">
                            h1,
                            h2,
                            h3 {
                                font-family: 'Open Sans';
                            }
                            table{
                                font-family: 'Open Sans';
                            }
                            td.model {
                                border: '2px';
                            }
                        </style>
                    </head>

                    <body>
                        <div class="title">
                            <table width="80%">
                                <tbody>
                                    <tr>
                                        <td >
                                            <h1 style="text-align:center; ">🔎 Tensorflow dGPU Unit Tests On DPCPP Nightly Report 🔍</h1>
                                        </td>
                                    </tr>
                                    <tr>
                                        <td>
                                            <h2 style="text-align:center; ">
                                                <span style="color:${COLOR}; font-size:30px; ">${RESULT}${EMOJI}</span>
                                            </h2>
                                        </td>
                                    </tr>
                                </tbody>
                            </table>
                        </div>

                        <div class="job_url ">
                            <h2>⌛Log url</h2>
                            <table width="80% " style="border:2px solid #000000;border-collapse: collapse; ">
                                <tbody>
                                    <tr >
                                        <th width="25% " style="border:2px solid #000000;text-align: center; background-color: #d3d3d3; ">Jenkins Job URL</th>
                                        <td width="75% " style="border:2px solid #000000;text-align: center; ">https://jenkins-aten-caffe2.sh.intel.com/job/Tensorflow_dGPU_DPCPP_Nightly/${currentBuild.number}</td>
                                    </tr>
                                </tbody>
                            </table>
                        </div>
                        <div class="code_info ">
                            <h2>Code Information</h2>
                            <table width=80% style="border:2px solid #000000;border-collapse: collapse; ">
                                <thead>
                                    <tr>
                                        <th width="25% " style="border:2px solid #000000;text-align: center;background-color: #d3d3d3; ">Commit ID</th>
                                        <th width="25% " style="border:2px solid #000000;text-align: center;background-color: #d3d3d3; ">Commit Author</th>
                                        <th width="25% " style="border:2px solid #000000;text-align: center;background-color: #d3d3d3; ">Commit Mail</th>
                                        <th width="25% " style="border:2px solid #000000;text-align: center;background-color: #d3d3d3; ">Commit Message</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <tr>
                                        <td style="border:2px solid #000000;text-align: center; ">${COMMIT_ID}</td>
                                        <td style="border:2px solid #000000;text-align: center; ">${COMMIT_OWNER_NAME}</td>
                                        <td style="border:2px solid #000000;text-align: center; ">${COMMIT_OWNER_EMAIL}</td>
                                        <td style="border:2px solid #000000;text-align: center; ">${COMMIT_SUBJECT}</td>
                                    </tr>
                                </tbody>
                            </table>
                        </div>

                        <div class="Summary ">
                            <h2>
                                Summary
                            </h2>
                            <table width="80% " style="border:2px solid #000000;border-collapse: collapse; ">
                                <thead>
                                    <th width="30% " style="border:2px solid #000000;text-align: center;background-color: #d3d3d3; "></th>
                                    <th width="30% " style="border:2px solid #000000;text-align: center;background-color: #d3d3d3; ">Today</th>
                                    <th width="30% " style="border:2px solid #000000;text-align: center;background-color: #d3d3d3; ">Last</th>
                                </thead>
                                <tbody>
                                    <tr>
                                        <td style="border:2px solid #000000;text-align: center; ">Failed UT number</td>
                                        <td style="border:2px solid #000000;text-align: center; ">${FAIL_NUM}</td>
                                        <td style="border:2px solid #000000;text-align: center; ">${FAIL_REFER_NUM}</td>
                                    </tr>
                                    <tr>
                                        <td style="border:2px solid #000000;text-align: center; ">Timeout UT number</td>
                                        <td style="border:2px solid #000000;text-align: center; ">${TIMEOUT_NUM}</td>
                                        <td style="border:2px solid #000000;text-align: center; ">${TIMEOUT_REFER_NUM}</td>
                                    </tr>
                                    <tr>
                                        <td style="border:2px solid #000000;text-align: center; ">Flaky UT number</td>
                                        <td style="border:2px solid #000000;text-align: center; ">${FLAKY_NUM}</td>
                                        <td style="border:2px solid #000000;text-align: center; ">${FLAKY_REFER_NUM}</td>
                                    </tr>
                                    <tr>
                                        <td style="border:2px solid #000000;text-align: center; ">Total UT number</td>
                                        <td style="border:2px solid #000000;text-align: center; ">${TOTAL_UT_NUM}</td>
                                        <td style="border:2px solid #000000;text-align: center; ">${REFER_TOTAL_UT_NUM}</td>
                                    </tr>
                                </tbody>
                            </table>
                        </div>
                        <div class="failed_cases ">
                            <h2>
                                UT Results
                            </h2>
                            <table width="80% " style="border:2px solid #000000;border-collapse: collapse; ">
                                <thead>
                                    <th width="20% " style="border:2px solid #000000;text-align: center;background-color: #d3d3d3; ">Type</th>
                                    <th width="40% " style="border:2px solid #000000;text-align: center;background-color: #d3d3d3; ">Today</th>
                                    <th width="40% " style="border:2px solid #000000;text-align: center;background-color: #d3d3d3; ">Last</th>
                                </thead>
                                <tbody>
                                    <tr>
                                        <td style="border:2px solid #000000;text-align: center; ">Fail Cases</td>
                                        <td style="border:2px solid #000000;text-align: left;padding-left:20px; ">
                                            ${FAIL_CASES}
                                        </td>
                                        <td style="border:2px solid #000000;text-align: left;padding-left:20px; ">
                                            ${FAIL_REFER}
                                        </td>
                                    </tr>
                                    <tr>
                                        <td style="border:2px solid #000000;text-align: center; ">Timeout Cases</td>
                                        <td style="border:2px solid #000000;text-align: left;padding-left:20px; ">
                                            ${TIMEOUT_CASES}
                                        </td>
                                        <td style="border:2px solid #000000;text-align: left;padding-left:20px; ">
                                            ${TIMEOUT_REFER}
                                        </td>
                                    </tr>
                                    <tr>
                                        <td style="border:2px solid #000000;text-align: center; ">Flaky Cases</td>
                                        <td style="border:2px solid #000000;text-align: left;padding-left:20px; ">
                                            ${FLAKY_CASES}
                                        </td>
                                        <td style="border:2px solid #000000;text-align: left;padding-left:20px; ">
                                            ${FLAKY_REFER}
                                        </td>
                                    </tr>
                                </tbody>
                            </table>
                        </div>
                """ + perf_table_html + accuracy_table_html + """
    </body>
</html>
                """
            )
        }
        } // if email
    }catch(Exception e ){
      println(e)
      RESULT = 'FAILURE'
      COLOR = "#FF0000"
      EMOJI = "✋"
      emailext(
                mimeType: 'text/html',
                from: 'Tensorflow_Nightly@intel.com',
                //to: 'maozhou.ge@intel.com;guizi.li@intel.com;shufan.wu@intel.com;cc:chuandongx.xu@intel.com;cc:baochenx.yang@intel.com;cc:fengxiax.wang@intel.com;karen.xiang@intel.com;cherry.zhang@intel.com;leicong.li@intel.com;yiqiang.li@intel.com;zhoulong.jiang@intel.com;quintin.wang@intel.com;shengzhi.chen@intel.com;' ,
                to: 'huimingx.song@intel.com',
                subject: '('+RESULT +') Tensorflow dGPU Unit Tests On DPCPP Nightly Test Report',
                body:"""
                <html>
                    <body>
                    <div class="title">
                            <table width="80%">
                                <tbody>
                                    <tr>
                                        <td>
                                            <h1 style="text-align:center; ">🔎 Tensorflow dGPU Unit Tests Nightly Report 🔍</h1>
                                        </td>
                                    </tr>
                                    <tr>
                                        <td>
                                            <h2 style="text-align:center; ">
                                                <span style="color:${COLOR}; font-size:30px; ">${RESULT}${EMOJI}</span>
                                            </h2>
                                        </td>
                                    </tr>
                                </tbody>
                            </table>
                        </div>
                        <div class="job_url ">
                            <h2>⌛Log url</h2>
                            <table width="80% " style="border:2px solid #000000;border-collapse: collapse; ">
                                <tbody>
                                    <tr >
                                        <th width="25% " style="border:2px solid #000000;text-align: center; background-color: #d3d3d3; ">Jenkins Job URL</th>
                                        <td width="75% " style="border:2px solid #000000;text-align: center; ">https://jenkins-aten-caffe2.sh.intel.com/job/Tensorflow_dGPU_DPCPP_Nightly/${currentBuild.number}</td>
                                    </tr>
                                </tbody>
                            </table>
                        </div>
                    </body>
                </html>
                """
            )
      sh '''
        exit 1
      '''
    }
}
