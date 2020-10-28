####### Print INFO Function ##########
# Print information when execution.
# Argument:     $1, information text.
# Env argument: VERBOSE, a global environment variable. set to 1 to print 
#               information, 0 to ignore print.
function INFO {
  VERBOSE=${VERBOSE:-0}
  local verbose=${VERBOSE}
  local info=${1}
  if [ "$verbose" == "1" ]; then
    echo "${info}"
  fi
}

######## BC return code to Bash return code ######
# 0 in bc is false, 1 is true
# opposite in bash 
##################################################

######## Check function definition ##############


# Accuracy check
function bert_base_fp32_chk {
  local log="${1}"
  local target=${2}
  acc=$(tail -n 20 ${log} | grep "eval_accuracy =" | tail -1 \
        | awk -F= '{print $2}' | tr -d ' ')
  INFO "DEBUG: $acc in log"
  INFO "DEBUG: target is $target"
  ret=$(bc <<< "
    scale=2;
    toleration = 0.05
    define abs(i) {
      if (i < 0) return (-i);
      return (i);
    };
    diff=abs(${acc} - ${target});
    diff < toleration;
    "
  )
  # Note: 0 in bc is false, 1 is true
  [ "${ret}" == "1" ]
}

function bert_base_fp16_chk {
  local log="${1}"
  local target=${2}
  local acc=$(tail -n 10 ${log} | grep "exact_match" | cut -d',' -f1 \
        | cut -d':' -f2 | tr -d ' ')
  local toleration=0.2
  INFO "DEBUG: $acc in log"
  INFO "DEBUG: target is $target"
  INFO "DEBUG: toleration is $toleration"
  local ret=$(bc <<< "
    scale=2;
    toleration = ${toleration}
    define abs(i) {
      if (i < 0) return (-i);
      return (i);
    };
    diff=abs(${acc} - ${target});
    diff < toleration;
    "
  )
  [ "${ret}" == "1" ]
}

function bert_base_bfp16_chk {
  local log="${1}"
  local target=${2}
  local acc=$(tail -n 10 ${log} | grep "exact_match" | cut -d',' -f1 \
        | cut -d':' -f2 | tr -d ' ')
  local toleration=0.2
  INFO "DEBUG: $acc in log"
  INFO "DEBUG: target is $target"
  INFO "DEBUG: toleration is $toleration"
  local ret=$(bc <<< "
    scale=2;
    toleration = ${toleration}
    define abs(i) {
      if (i < 0) return (-i);
      return (i);
    };
    diff=abs(${acc} - ${target});
    diff < toleration;
    "
  )
  [ "${ret}" == "1" ]
}

function resnet50_v1_fp32_chk {
  local log="${1}"
  local target=${2}
  local acc=$(tail -n 10 ${log} | grep "Top1 accuracy, Top5 accuracy" \
              | tail -1 \
              | grep -Po "\(.*?\)" | tail -1 | tr -d '() ' | cut -d',' -f1)
  local toleration=0.03826
  INFO "DEBUG: $acc in log"
  INFO "DEBUG: target is $target"
  INFO "DEBUG: toleration is $toleration"
  local ret=$(bc <<< "
    scale=2;
    toleration = ${toleration}
    define abs(i) {
      if (i < 0) return (-i);
      return (i);
    };
    diff=abs(${acc} - ${target});
    diff < toleration;
    "
  )
  [ "${ret}" == "1" ]
}

function resnet50_v1_fp16_chk {
  local log="${1}"
  local target=${2}
  local acc=$(tail -n 10 ${log} | grep "Top1 accuracy, Top5 accuracy" \
              | tail -1 \_log
              | grep -Po "\(.*?\)" | tail -1 | tr -d '() ' | cut -d',' -f1)
  local toleration=0.03826
  INFO "DEBUG: $acc in log"
  INFO "DEBUG: target is $target"
  INFO "DEBUG: toleration is $toleration"
  local ret=$(bc <<< "
    scale=2;
    toleration = ${toleration}
    define abs(i) {
      if (i < 0) return (-i);
      return (i);
    };
    diff=abs(${acc} - ${target});
    diff < toleration;
    "
  )
  [ "${ret}" == "1" ]
}

function resnet50_v1_bfp16_chk {
  local log="${1}"
  local target=${2}
  local acc=$(tail -n 10 ${log} | grep "Top1 accuracy, Top5 accuracy" \
              | tail -1 \
              | grep -Po "\(.*?\)" | tail -1 | tr -d '() ' | cut -d',' -f1)
  local toleration=0.03826
  INFO "DEBUG: $acc in log"
  INFO "DEBUG: target is $target"
  INFO "DEBUG: toleration is $toleration"
  local ret=$(bc <<< "
    scale=2;
    toleration = ${toleration}
    define abs(i) {
      if (i < 0) return (-i);
      return (i);
    };
    diff=abs(${acc} - ${target});
    diff < toleration;
    "
  )
  [ "${ret}" == "1" ]
}


# Latency

# Throughput
function throughput_resnet50_v1_fp32_legacy_chk {
  local log="${1}"
  local target=${2:-0.0001}
  local throughput=$(tail -n 10 ${log} | grep ^Throughput: | cut -d' ' -f2)
  local threshold=0.05
  local ret=$(bc <<< "
    scale=4;
    threshold = ${threshold}
    define abs(i) {
      if (i < 0) return (-i);
      return (i);
    };
    diff=abs(${throughput} - ${target}) / ${target};
    diff < threshold;
    "
  )
  [ "${ret}" == "1" ]
}

function throughput_resnet50_v1_fp16_legacy_chk {
  local log="${1}"
  local target=${2:-0.0001}
  local throughput=$(tail -n 10 ${log} | grep ^Throughput: | cut -d' ' -f2)
  local threshold=0.05
  local ret=$(bc <<< "
    scale=4;
    threshold = ${threshold}
    define abs(i) {
      if (i < 0) return (-i);
      return (i);
    };
    diff=abs(${throughput} - ${target}) / ${target};
    diff < threshold;
    "
  )
  [ "${ret}" == "1" ]
}

function throughput_resnet50_v1_bfp16_legacy_chk {
  local log="${1}"
  local target=${2:-0.0001}
  local throughput=$(tail -n 10 ${log} | grep ^Throughput: | cut -d' ' -f2)
  local threshold=0.05
  local ret=$(bc <<< "
    scale=4;
    threshold = ${threshold}
    define abs(i) {
      if (i < 0) return (-i);
      return (i);
    };
    diff=abs(${throughput} - ${target}) / ${target};
    diff < threshold;
    "
  )
  [ "${ret}" == "1" ]
}

function throughput_resnet50_v1_int8_legacy_chk {
  [ "${ret}" == "1" ]
}

function accuracy_resnet50_v1_fp32_legacy_chk {
  local log="${1}"
  local log_ref="${2}"
  local acc=$(cat ${log} | grep "Top1 accuracy, Top5 accuracy" \
              | tail -1 \
              | grep -Po "\(.*?\)" | tail -1 | tr -d '() ' | cut -d',' -f1)
  local acc_ref=$(cat ${log_ref} | grep "Top1 accuracy, Top5 accuracy" \
              | tail -1 \
              | grep -Po "\(.*?\)" | tail -1 | tr -d '() ' | cut -d',' -f1)
  INFO "accuracy in log: $acc"
  INFO "accuracy in log_ref: $acc2"
  local ret=$(
    bc <<< "
      scale=2;
      define abs(i) {
        if (i < 0) return (-i);
        return (i);
      };
      diff=abs(${acc} - ${acc_ref}) / ${acc_ref};
      diff < 0.05;
    "
  )
  [ "${ret}" == "1" ]
}

function accuracy_bert_fp32_legacy_chk {
  local log="${1}"
  local log_ref="${2}"
  local acc=$(cat ${log} | grep "eval_accuracy =" | tail -1 \
        | awk -F= '{print $2}' | tr -d ' ')
  local acc_ref=$(cat ${log_ref} | grep "eval_accuracy =" | tail -1 \
        | awk -F= '{print $2}' | tr -d ' ')
  INFO "accuracy in log: $acc"
  INFO "accuracy in log_ref: $acc2"
  local ret=$(
    bc <<< "
      scale=2;
      define abs(i) {
        if (i < 0) return (-i);
        return (i);
      };
      diff=abs(${acc} - ${acc_ref}) / ${acc_ref};
      diff < 0.05;
    "
  )
  [ "${ret}" == "1" ]
}

function accuracy_resnet50_v1_fp32_chk {
  local log="${1}"
  local acc=$(cat ${log} | grep "Top1 accuracy, Top5 accuracy" \
              | tail -1 \
              | grep -Po "\(.*?\)" | tail -1 | tr -d '() ' | cut -d',' -f1)
  INFO "accuracy:$acc"
}

function accuracy_bert_fp32_chk {
  local log="${1}"
  local acc=$(cat ${log} | grep "eval_accuracy =" | tail -1 \
        | awk -F= '{print $2}' | tr -d ' ')
  INFO "accuracy:$acc"
}

function accuracy_bert_base_fp32_chk {
  local log="${1}"
  local acc=$(cat ${log} | grep "eval_accuracy =" | tail -1 \
        | awk -F= '{print $2}' | tr -d ' ')
  INFO "accuracy:$acc"
}

#loss = 9.687743, step = 9 (17.915 sec)
function accuracy_transformer_lt_bfp16_chk {
  local log="${1}"
  local acc=$(cat ${log} | grep "eval_accuracy =" | tail -1 \
        | awk -F= '{print $2}' | tr -d ' ')
  INFO "accuracy:$acc"
}
