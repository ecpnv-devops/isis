#!/usr/bin/env bash
usage() {
  echo "$(basename $0): [-A] [-P] [-q] message" >&2
  echo "  -A : suppress adding automatically, ie don't call 'git add .'" >&2
  echo "  -p : also push" >&2
  echo "  -q : [quick] - build but don't run tests" >&2
}

QUICK=""
PUSH=""
ADD=""

while getopts ":hApq" arg; do
  case $arg in
    h)
      usage
      exit 0
      ;;
    A)
      ADD="no-add"
      ;;
    p)
      PUSH="push"
      ;;
    q)
      QUICK=" [quick]"
      ;;
    *)
      usage
      exit 1
  esac
done

if [ $# -lt 1 ];
then
  usage
  exit 1
fi

shift $((OPTIND-1))

ISSUE=$(git rev-parse --abbrev-ref HEAD | cut -d- -f1,2)
MSG=$*

echo "     ISSUE     : ${ISSUE}"
echo "     MSG       : ${MSG}"
echo "-A : (NO-)ADD  : ${ADD}"
echo "-p : PUSH      : ${PUSH}"
echo "-q : QUICK     : ${QUICK}"

if [ -z "$ADD" ]
then
  git add .
fi
git commit -m "$ISSUE: ${MSG}${QUICK}${SKIP}${V2}"
if [ -n "$PUSH" ]
then
  git push
fi
