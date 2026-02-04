# Cleanup handler registry
# Allows multiple cleanup functions to be registered and called on exit

# Array to store registered cleanup function names
VIASH_CLEANUP_HANDLERS=()

# ViashRegisterCleanup: Register a cleanup function to be called on exit
# $1: Name of the function to call during cleanup
# usage: ViashRegisterCleanup my_cleanup_function
function ViashRegisterCleanup {
  local handler="$1"
  VIASH_CLEANUP_HANDLERS+=("$handler")
}

# ViashRunCleanupHandlers: Run all registered cleanup handlers in reverse order
# This function is meant to be used as the EXIT trap handler
function ViashRunCleanupHandlers {
  # Run handlers in reverse order (LIFO - last registered runs first)
  local i
  for (( i=${#VIASH_CLEANUP_HANDLERS[@]}-1 ; i>=0 ; i-- )); do
    local handler="${VIASH_CLEANUP_HANDLERS[$i]}"
    if type "$handler" &>/dev/null; then
      ViashDebug "Running cleanup handler: $handler"
      "$handler"
    fi
  done
}

# Set up the master EXIT trap that runs all registered handlers
trap ViashRunCleanupHandlers EXIT
